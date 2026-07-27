package co.branch.jsonlogic.parity

/**
 * The one difference the differential fuzzer tolerates, and only because it is not a property of
 * either engine: `substr` renders its first argument without a null check — deliberately, in both —
 * so a null subject makes both throw `java.lang.NullPointerException` from that one statement, and
 * the Java engine's exception sometimes carries a message the JVM synthesizes from *its* bytecode
 * ([SYNTHESIZED_SUBSTR_NPE_TEXT]), which the port's null check cannot produce.
 *
 * Nothing in either engine authors that text, and the JVM does not attach it consistently: it stops
 * appearing once the throw site is hot, so the identical case reports it at the start of a run and
 * reports no message at all later in the same run — a sweep of eight generator seeds produced around
 * 100 of these per 20 000 cases for whichever seeds ran first and none for the rest, following the
 * position in the run rather than the seed. Running the JVM with
 * `-XX:-ShowCodeDetailsInExceptionMessages` removes the text entirely, leaving both engines reporting
 * null.
 *
 * What the engines do author — the type, message and jsonPath of a JsonLogicException — is compared in
 * full, as is every other exception both raise out of the same JVM call, such as the
 * `StringIndexOutOfBoundsException` from `substr`'s range arithmetic. No fixture case reaches this
 * path, so the acceptance gate is unaffected.
 */
internal fun isKnownSubstrNullPointerPair(oracleError: Throwable, portedError: Throwable): Boolean {
    if (oracleError.javaClass != NullPointerException::class.java) return false
    if (portedError.javaClass != NullPointerException::class.java) return false

    // The port's null check never produces a message; the Java engine's either carries the JVM's
    // synthesized text or, once the site is hot, nothing.
    if (portedError.message != null) return false
    if (oracleError.message != null && oracleError.message != SYNTHESIZED_SUBSTR_NPE_TEXT) return false

    return oracleError.originatesIn(ORACLE_SUBSTR_FRAME) && portedError.originatesIn(PORTED_SUBSTR_FRAME)
}

/**
 * Whether the exception was raised by [frame] itself, rather than merely somewhere beneath it: an
 * operator's frame is still on the stack while its arguments are being evaluated, so a failure inside
 * a nested operator would also carry it. Only the top frame identifies where a failure came from.
 *
 * A throwable with no stack trace at all — which is how the JVM reports an implicit exception from a
 * hot site, and which is also the state in which the Java engine's message goes missing — originates
 * nowhere as far as this can tell, and is therefore never tolerated.
 */
private fun Throwable.originatesIn(frame: String): Boolean =
    stackTrace.firstOrNull()?.let { "${it.className}.${it.methodName}" } == frame

internal const val ORACLE_SUBSTR_FRAME =
    "io.github.jamsesso.jsonlogic.evaluator.expressions.SubstringExpression.evaluate"

internal const val PORTED_SUBSTR_FRAME =
    "co.branch.jsonlogic.evaluator.expressions.SubstringExpression.evaluate"

internal const val SYNTHESIZED_SUBSTR_NPE_TEXT =
    "Cannot invoke \"Object.toString()\" because the return value of \"java.util.List.get(int)\" is null"
