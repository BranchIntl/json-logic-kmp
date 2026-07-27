package co.branch.jsonlogic.parity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import co.branch.jsonlogic.JsonLogic as PortedJsonLogic
import io.github.jamsesso.jsonlogic.JsonLogic as OracleJsonLogic

/**
 * Pins what [isKnownSubstrNullPointerPair] will and will not wave through, since anything it waves
 * through is a difference between the two engines that the fuzzer will never report.
 *
 * The real `substr` failure is run through both engines to keep the frame names and messages the
 * waiver matches on tied to what the engines actually do; the rest of the cases are built by hand,
 * because the JVM attaches its synthesized message only while the throw site is cold and a test may
 * not assume it runs first.
 */
class NullPointerToleranceTest {

    @Test
    fun theRealSubstrFailureOriginatesInTheFramesTheWaiverRequires() {
        val (oracleError, portedError) = runBothEngines("""{"substr": [null, 0]}""")

        assertEquals<Class<*>>(NullPointerException::class.java, oracleError.javaClass)
        assertEquals<Class<*>>(NullPointerException::class.java, portedError.javaClass)
        assertEquals(null, portedError.message, "the port's null check is not expected to carry a message")
        assertEquals(PORTED_SUBSTR_FRAME, topFrameOf(portedError))
        // The Java engine's implicit null check reports no frames at all once the site is hot, which is
        // also when its message goes missing; while it does report them, they have to be substr's.
        if (oracleError.stackTrace.isNotEmpty()) {
            assertEquals(ORACLE_SUBSTR_FRAME, topFrameOf(oracleError))
        }
        assertTrue(
            oracleError.message == null || oracleError.message == SYNTHESIZED_SUBSTR_NPE_TEXT,
            "unrecognized message on the Java engine's substr failure: ${oracleError.message}",
        )
    }

    @Test
    fun theRealSubstrFailureIsNeverReportedAsADivergence() {
        val rule = """{"substr": [null, 0]}"""
        val ruleElement = Json.parseToJsonElement(rule)
        val dataElement = Json.parseToJsonElement("null")
        val oracleCase = oracleCaseOf(ruleElement, dataElement)
        val oracleOutcome = outcomeOf { OracleJsonLogic().apply(oracleCase.rule, oracleCase.data) }
        val portedOutcome = outcomeOf { PortedJsonLogic().apply(ruleElement, dataElement) }
        val oracleError = assertIs<Outcome.Threw>(oracleOutcome).error
        val portedError = assertIs<Outcome.Threw>(portedOutcome).error

        // Either the two failures already agree — which they do once the JVM stops synthesizing its
        // message — or the waiver recognizes the pair. The fuzzer must never have to report it.
        val agreed = disagreementBetween(oracleOutcome, portedOutcome) == null
        assertTrue(
            agreed || isKnownSubstrNullPointerPair(oracleError, portedError),
            "the known substr failure was reported as a divergence: " +
                "java=${oracleError.message} at ${topFrameOf(oracleError)}, " +
                "kotlin=${portedError.message} at ${topFrameOf(portedError)}",
        )
    }

    @Test
    fun aSubstrPairCarryingEitherKnownMessageIsTolerated() {
        assertTrue(
            isKnownSubstrNullPointerPair(
                nullPointerException(SYNTHESIZED_SUBSTR_NPE_TEXT, ORACLE_SUBSTR_FRAME),
                nullPointerException(null, PORTED_SUBSTR_FRAME),
            ),
        )
        assertTrue(
            isKnownSubstrNullPointerPair(
                nullPointerException(null, ORACLE_SUBSTR_FRAME),
                nullPointerException(null, PORTED_SUBSTR_FRAME),
            ),
        )
    }

    @Test
    fun aCrossSiteNullPointerPairIsNotTolerated() {
        // Each failure came out of a different operator. substr's frame is still on the stack beneath
        // the one that failed, because an operator's own frame stays there while its arguments are
        // evaluated — which is why only the top frame counts.
        assertFalse(
            isKnownSubstrNullPointerPair(
                nullPointerException(SYNTHESIZED_SUBSTR_NPE_TEXT, ORACLE_CAT_FRAME, ORACLE_SUBSTR_FRAME),
                nullPointerException(null, PORTED_SUBSTR_FRAME),
            ),
            "a failure inside cat was tolerated as substr's",
        )
        assertFalse(
            isKnownSubstrNullPointerPair(
                nullPointerException(SYNTHESIZED_SUBSTR_NPE_TEXT, ORACLE_SUBSTR_FRAME),
                nullPointerException(null, PORTED_CAT_FRAME, PORTED_SUBSTR_FRAME),
            ),
            "the two engines failing at different operators was tolerated",
        )
    }

    @Test
    fun anUnrecognizedNullPointerMessageIsNotTolerated() {
        assertFalse(
            isKnownSubstrNullPointerPair(
                nullPointerException("boom", ORACLE_SUBSTR_FRAME),
                nullPointerException(null, PORTED_SUBSTR_FRAME),
            ),
            "an arbitrary message on the Java engine's failure was tolerated",
        )
        assertFalse(
            isKnownSubstrNullPointerPair(
                nullPointerException(SYNTHESIZED_SUBSTR_NPE_TEXT, ORACLE_SUBSTR_FRAME),
                nullPointerException("boom", PORTED_SUBSTR_FRAME),
            ),
            "a message on the port's failure was tolerated",
        )
    }

    @Test
    fun aNullPointerExceptionWithoutFramesIsNotTolerated() {
        assertFalse(
            isKnownSubstrNullPointerPair(
                nullPointerException(SYNTHESIZED_SUBSTR_NPE_TEXT),
                nullPointerException(null, PORTED_SUBSTR_FRAME),
            ),
        )
    }

    @Test
    fun onlyPlainNullPointerExceptionsAreTolerated() {
        class Subclass(message: String?) : NullPointerException(message)

        val subclass = Subclass(null).apply { stackTrace = framesOf(PORTED_SUBSTR_FRAME) }
        assertFalse(
            isKnownSubstrNullPointerPair(
                nullPointerException(SYNTHESIZED_SUBSTR_NPE_TEXT, ORACLE_SUBSTR_FRAME),
                subclass,
            ),
        )
    }

    @Test
    fun theWaiverNamesMethodsBothEnginesStillHave() {
        // A rename in either engine would otherwise leave the waiver quietly matching nothing, or —
        // worse, if a class were renamed into another's place — the wrong thing.
        for (frame in listOf(ORACLE_SUBSTR_FRAME, PORTED_SUBSTR_FRAME, ORACLE_CAT_FRAME, PORTED_CAT_FRAME)) {
            val type = Class.forName(frame.substringBeforeLast('.'))
            val method = frame.substringAfterLast('.')
            assertTrue(
                type.declaredMethods.any { it.name == method },
                "$frame no longer names a method that exists",
            )
        }
    }

    private fun runBothEngines(rule: String): Pair<Throwable, Throwable> {
        val ruleElement: JsonElement = Json.parseToJsonElement(rule)
        val dataElement = Json.parseToJsonElement("null")
        val oracleCase = oracleCaseOf(ruleElement, dataElement)
        val oracle = outcomeOf { OracleJsonLogic().apply(oracleCase.rule, oracleCase.data) }
        val ported = outcomeOf { PortedJsonLogic().apply(ruleElement, dataElement) }

        return assertIs<Outcome.Threw>(oracle).error to assertIs<Outcome.Threw>(ported).error
    }

    private fun nullPointerException(message: String?, vararg frames: String): NullPointerException =
        NullPointerException(message).apply { stackTrace = framesOf(*frames) }

    private fun framesOf(vararg frames: String): Array<StackTraceElement> = frames
        .map { StackTraceElement(it.substringBeforeLast('.'), it.substringAfterLast('.'), null, -1) }
        .toTypedArray()

    private fun topFrameOf(error: Throwable): String? =
        error.stackTrace.firstOrNull()?.let { "${it.className}.${it.methodName}" }

    private companion object {
        const val ORACLE_CAT_FRAME =
            "io.github.jamsesso.jsonlogic.evaluator.expressions.ConcatenateExpression.evaluate"
        const val PORTED_CAT_FRAME =
            "co.branch.jsonlogic.evaluator.expressions.ConcatenateExpression.evaluate"
    }
}
