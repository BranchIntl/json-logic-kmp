package co.branch.jsonlogic.parity

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import co.branch.jsonlogic.JsonLogic as PortedJsonLogic
import io.github.jamsesso.jsonlogic.JsonLogic as OracleJsonLogic

/**
 * Pins what the fuzzer will and will not pass over, through [fuzzVerdict] — the same decision the
 * fuzzer makes, diff included, rather than the tolerance in isolation. Testing the tolerance alone
 * misses the case that matters most: two null dereferences carrying no message have identical
 * signatures whichever expression raised them, so the diff has to be the one that tells them apart.
 *
 * The pairs are built by hand because the JVM's account of a null dereference is not something a test
 * can arrange, and two real cases anchor the hand-built ones to what the engines actually produce.
 */
class NullPointerToleranceTest {

    @Test
    fun theSameExpressionFailingWithNoMessagesAgrees() {
        assertEquals(
            FuzzVerdict.Agreed,
            fuzzVerdict(threw(npe(null, ORACLE_CAT)), threw(npe(null, PORTED_CAT))),
        )
        assertEquals(
            FuzzVerdict.Agreed,
            fuzzVerdict(threw(npe(null, ORACLE_SUBSTR)), threw(npe(null, PORTED_SUBSTR))),
        )
    }

    @Test
    fun theSameExpressionFailingWithTheSynthesizedTextIsTolerated() {
        assertEquals(
            FuzzVerdict.Tolerated("SubstringExpression"),
            fuzzVerdict(threw(npe(SYNTHESIZED_TEXT, ORACLE_SUBSTR)), threw(npe(null, PORTED_SUBSTR))),
        )
        // The JVM describes whichever dereference it can, not just substr's.
        assertEquals(
            FuzzVerdict.Tolerated("ConcatenateExpression"),
            fuzzVerdict(threw(npe(SYNTHESIZED_TEXT, ORACLE_CAT)), threw(npe(null, PORTED_CAT))),
        )
    }

    @Test
    fun differentExpressionsFailingWithNoMessagesDiverge() {
        // Both carry the same type, no message and no jsonPath: only the origin separates them.
        assertEquals(
            FuzzVerdict.Diverged(DisagreementKind.ERRORS_DIFFER),
            fuzzVerdict(threw(npe(null, ORACLE_CAT)), threw(npe(null, PORTED_SUBSTR))),
        )
        assertEquals(
            FuzzVerdict.Diverged(DisagreementKind.ERRORS_DIFFER),
            fuzzVerdict(threw(npe(null, ORACLE_SUBSTR)), threw(npe(null, PORTED_CAT))),
        )
    }

    @Test
    fun differentExpressionsFailingWithTheSynthesizedTextDiverge() {
        assertEquals(
            FuzzVerdict.Diverged(DisagreementKind.ERRORS_DIFFER),
            fuzzVerdict(threw(npe(SYNTHESIZED_TEXT, ORACLE_CAT)), threw(npe(null, PORTED_SUBSTR))),
        )
    }

    @Test
    fun anUnplaceableFailureDiverges() {
        assertEquals(
            FuzzVerdict.Diverged(DisagreementKind.ERRORS_DIFFER),
            fuzzVerdict(threw(npe(SYNTHESIZED_TEXT)), threw(npe(null, PORTED_SUBSTR))),
            "a Java-engine failure with no frames of its own was placed at the port's expression",
        )
        assertEquals(
            FuzzVerdict.Diverged(DisagreementKind.ERRORS_DIFFER),
            fuzzVerdict(threw(npe(SYNTHESIZED_TEXT, ORACLE_SUBSTR)), threw(npe(null))),
        )
        // Frames, but none of the engine's own.
        assertEquals(
            FuzzVerdict.Diverged(DisagreementKind.ERRORS_DIFFER),
            fuzzVerdict(threw(npe(null, JDK_STREAM_FRAME)), threw(npe(null, PORTED_SUBSTR))),
        )
    }

    @Test
    fun anUnrecognizedMessageDiverges() {
        assertEquals(
            FuzzVerdict.Diverged(DisagreementKind.ERRORS_DIFFER),
            fuzzVerdict(threw(npe("boom", ORACLE_SUBSTR)), threw(npe(null, PORTED_SUBSTR))),
        )
        assertEquals(
            FuzzVerdict.Diverged(DisagreementKind.ERRORS_DIFFER),
            fuzzVerdict(threw(npe(SYNTHESIZED_TEXT, ORACLE_SUBSTR)), threw(npe("boom", PORTED_SUBSTR))),
        )
    }

    @Test
    fun aNullPointerSubclassIsComparedOnItsSignature() {
        class Subclass : NullPointerException()

        val subclass = Subclass().apply { stackTrace = framesOf(PORTED_SUBSTR) }
        assertEquals(
            FuzzVerdict.Diverged(DisagreementKind.ERRORS_DIFFER),
            fuzzVerdict(threw(npe(null, ORACLE_SUBSTR)), threw(subclass)),
        )
    }

    @Test
    fun theTopmostEngineFrameDecidesTheOrigin() {
        // The Java engine's cat fails inside a stream pipeline, in a lambda of its own class; neither
        // the JDK frames above it nor the lambda's synthetic name should hide where it came from.
        val oracleError = npe(null, JDK_STREAM_FRAME, "$ORACLE_CAT\$\$Lambda\$42", ORACLE_SUBSTR)
        assertEquals("ConcatenateExpression", oracleOriginOf(oracleError))
        assertEquals(
            FuzzVerdict.Agreed,
            fuzzVerdict(threw(oracleError), threw(npe(null, PORTED_CAT))),
        )
    }

    @Test
    fun theSynthesizedShapeAcceptsOnlyTheJvmsOwnAccount() {
        assertTrue(isJvmSynthesizedNullPointerText(SYNTHESIZED_TEXT))
        assertTrue(isJvmSynthesizedNullPointerText("""Cannot read field "x" because "y" is null"""))
        assertFalse(isJvmSynthesizedNullPointerText(""))
        assertFalse(isJvmSynthesizedNullPointerText("boom"))
        assertFalse(isJvmSynthesizedNullPointerText("substr expects 2 or 3 arguments"))
        assertFalse(isJvmSynthesizedNullPointerText("Cannot invoke something"))
    }

    @Test
    fun theRealSubstrFailureIsTolerated() {
        val verdict = verdictFor("""{"substr": [null, 0]}""", "null")

        assertEquals(FuzzVerdict.Tolerated("SubstringExpression"), verdict, flagAdvice())
    }

    @Test
    fun theRealCatFailureAgrees() {
        val verdict = verdictFor("""{"cat": [{"var": "z"}]}""", "{}")

        assertEquals(FuzzVerdict.Agreed, verdict, flagAdvice())
    }

    @Test
    fun bothEnginesStillNameTheExpressionClassesThesePairsAreBuiltFrom() {
        // A rename would otherwise leave the hand-built pairs describing origins that cannot occur.
        for (className in listOf(ORACLE_CAT, PORTED_CAT, ORACLE_SUBSTR, PORTED_SUBSTR)) {
            assertEquals(className, Class.forName(className).name)
        }
    }

    private fun verdictFor(rule: String, data: String): FuzzVerdict {
        val ruleElement = Json.parseToJsonElement(rule)
        val dataElement = Json.parseToJsonElement(data)
        val oracleCase = oracleCaseOf(ruleElement, dataElement)
        val oracle = outcomeOf { OracleJsonLogic().apply(oracleCase.rule, oracleCase.data) }
        val ported = outcomeOf { PortedJsonLogic().apply(ruleElement, dataElement) }
        assertIs<Outcome.Threw>(oracle, "the Java engine was expected to fail on $rule")
        assertIs<Outcome.Threw>(ported, "the port was expected to fail on $rule")

        return fuzzVerdict(oracle, ported)
    }

    /**
     * Both real cases depend on every throwable carrying its own stack trace, which the JVM only
     * guarantees with the flag the `jvmTest` task sets.
     */
    private fun flagAdvice(): String {
        val flag = "-XX:-OmitStackTraceInFastThrow"
        val arguments = java.lang.management.ManagementFactory.getRuntimeMXBean().inputArguments

        return if (arguments.any { flag in it }) {
            "running with $flag, so every failure should have been placeable"
        } else {
            "this JVM was started without $flag, so the JVM may have answered a hot throw site with a " +
                "traceless shared instance; run this through the jvmTest task"
        }
    }

    private fun threw(error: Throwable): Outcome<Nothing> = Outcome.Threw(error)

    private fun npe(message: String?, vararg classNames: String): NullPointerException =
        NullPointerException(message).apply { stackTrace = framesOf(*classNames) }

    private fun framesOf(vararg classNames: String): Array<StackTraceElement> = classNames
        .map { StackTraceElement(it, "evaluate", null, -1) }
        .toTypedArray()

    private companion object {
        const val ORACLE_CAT = "io.github.jamsesso.jsonlogic.evaluator.expressions.ConcatenateExpression"
        const val PORTED_CAT = "co.branch.jsonlogic.evaluator.expressions.ConcatenateExpression"
        const val ORACLE_SUBSTR = "io.github.jamsesso.jsonlogic.evaluator.expressions.SubstringExpression"
        const val PORTED_SUBSTR = "co.branch.jsonlogic.evaluator.expressions.SubstringExpression"
        const val JDK_STREAM_FRAME = "java.util.stream.ReferencePipeline\$3\$1"
        const val SYNTHESIZED_TEXT =
            "Cannot invoke \"Object.toString()\" because the return value of \"java.util.List.get(int)\" is null"
    }
}
