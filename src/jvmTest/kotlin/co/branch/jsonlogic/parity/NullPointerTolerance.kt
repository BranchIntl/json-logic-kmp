package co.branch.jsonlogic.parity

import kotlinx.serialization.json.JsonElement

/**
 * How a pair of plain `java.lang.NullPointerException`s relate.
 *
 * Neither engine ever constructs one — there is no `NullPointerException` anywhere in either source
 * tree — so each is the JVM's report of a null dereference, and its message is either nothing or the
 * text the JVM synthesizes from the bytecode that failed. Over half a million generated cases, the
 * Java engine produced exactly one distinct message and the port none at all, which leaves the message
 * unable to tell two such failures apart: a pair raised by entirely different operators carries the
 * same exception type, no message and no jsonPath, and would compare as identical. Which expression
 * raised each failure is therefore established before anything else about it is compared.
 */
internal sealed interface NullPointerPairing {

    /** Raised by the same expression, neither carrying a message: nothing distinguishes them. */
    data class Identical(val origin: String) : NullPointerPairing

    /**
     * Raised by the same expression, with the JVM's synthesized text on the Java engine's side only.
     *
     * Nothing in either engine authors that text and the port cannot produce it at all, so this is the
     * JVM describing one engine's bytecode and not the other's. It is not even a stable property of the
     * Java engine: running without `-XX:-OmitStackTraceInFastThrow`, the JVM answers a hot throw site
     * with a shared traceless instance carrying no message, and the same case reports the text early in
     * a run and nothing later in it. The differential fuzzer tolerates this pairing and counts it; the
     * acceptance gate does not have to, since no fixture case reaches a null dereference.
     */
    data class SynthesizedTextOnly(val origin: String) : NullPointerPairing

    /**
     * Raised by different expressions, or by something these cannot place, or carrying a message
     * neither the JVM's synthesis nor either engine explains.
     */
    data object Different : NullPointerPairing
}

internal fun isPlainNullPointerException(error: Throwable): Boolean =
    error.javaClass == NullPointerException::class.java

internal fun pairNullPointers(oracleError: Throwable, portedError: Throwable): NullPointerPairing {
    val oracleOrigin = oracleOriginOf(oracleError)
    val portedOrigin = portedOriginOf(portedError)

    // An unplaceable origin is not a match: a throwable carrying no frames of its own engine cannot be
    // shown to share an origin with anything, and taking that on trust is what this exists to stop.
    if (oracleOrigin == null || portedOrigin == null || oracleOrigin != portedOrigin) {
        return NullPointerPairing.Different
    }

    // The port's null checks never carry a message, at any site.
    if (portedError.message != null) return NullPointerPairing.Different

    val oracleMessage = oracleError.message ?: return NullPointerPairing.Identical(oracleOrigin)

    return if (isJvmSynthesizedNullPointerText(oracleMessage)) {
        NullPointerPairing.SynthesizedTextOnly(oracleOrigin)
    } else {
        NullPointerPairing.Different
    }
}

/**
 * The simple name of the expression class a failure came out of: the topmost frame belonging to the
 * engine that raised it, which is not the topmost frame overall — the Java engine's `cat` fails inside
 * a stream pipeline several JDK frames deep, and the port's inside its own rendering helper.
 *
 * The two engines' classes carry the same simple names under different packages, which is what makes
 * their origins comparable at all. A synthetic suffix (`ConcatenateExpression$$Lambda$42`) is dropped,
 * so a failure inside an operator's lambda is placed at the operator.
 */
internal fun oracleOriginOf(error: Throwable): String? = originOf(error, ORACLE_PACKAGE)

internal fun portedOriginOf(error: Throwable): String? = originOf(error, PORTED_PACKAGE)

private fun originOf(error: Throwable, enginePackage: String): String? = error.stackTrace
    .firstOrNull { it.className.startsWith(enginePackage) && !it.className.startsWith(HARNESS_PACKAGE) }
    ?.className
    ?.substringAfterLast('.')
    ?.substringBefore('$')

/**
 * Whether [message] has the shape the JVM gives a null dereference it can describe: what it could not
 * do, and which expression was null — `Cannot invoke "Object.toString()" because the return value of
 * "java.util.List.get(int)" is null`. Since neither engine writes `NullPointerException` messages, this
 * confirms the text really is the JVM's rather than guarding against an engine-authored one.
 */
internal fun isJvmSynthesizedNullPointerText(message: String): Boolean =
    message.startsWith("Cannot ") && " because " in message && message.endsWith("is null")

/** The differential fuzzer's decision about one case. */
internal sealed interface FuzzVerdict {

    /** The two engines did the same thing. */
    data object Agreed : FuzzVerdict

    /** They differed only in the JVM's account of a null dereference both made at [origin]. */
    data class Tolerated(val origin: String) : FuzzVerdict

    /** A difference to report. */
    data class Diverged(val kind: DisagreementKind) : FuzzVerdict
}

/**
 * The whole of the fuzzer's decision for one case: the shared diff first, then the single tolerance the
 * fuzzer applies and the acceptance gate does not.
 *
 * The tolerance can only ever downgrade something [disagreementBetween] already called a disagreement,
 * and it re-establishes the pairing itself, so no case reaches agreement here that the diff would not
 * have agreed on for a reason of its own.
 */
internal fun fuzzVerdict(oracle: Outcome<Any?>, ported: Outcome<JsonElement>): FuzzVerdict {
    val kind = disagreementBetween(oracle, ported) ?: return FuzzVerdict.Agreed
    val pairing = nullPointerPairingOf(oracle, ported)

    return if (pairing is NullPointerPairing.SynthesizedTextOnly) {
        FuzzVerdict.Tolerated(pairing.origin)
    } else {
        FuzzVerdict.Diverged(kind)
    }
}

/** The pairing of two outcomes that are both plain [NullPointerException]s, and null otherwise. */
internal fun nullPointerPairingOf(oracle: Outcome<*>, ported: Outcome<*>): NullPointerPairing? {
    val oracleError = (oracle as? Outcome.Threw)?.error ?: return null
    val portedError = (ported as? Outcome.Threw)?.error ?: return null
    if (!isPlainNullPointerException(oracleError) || !isPlainNullPointerException(portedError)) return null

    return pairNullPointers(oracleError, portedError)
}

private const val ORACLE_PACKAGE = "io.github.jamsesso.jsonlogic."
private const val PORTED_PACKAGE = "co.branch.jsonlogic."

/** This harness shares the port's package root, and its own frames never place an engine's failure. */
private const val HARNESS_PACKAGE = "co.branch.jsonlogic.parity."
