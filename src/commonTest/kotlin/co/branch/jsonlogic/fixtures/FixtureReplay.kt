package co.branch.jsonlogic.fixtures

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Outcome of running a case through [FixtureReplay.runValues] or [FixtureReplay.runErrors]. */
data class ReplayResult(
    val ran: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val failures: List<String>,
)

/** The engine-reported error for one error-fixture case; produced by the caller's own catch. */
data class ErrorOutcome(val message: String, val jsonPath: String)

/**
 * Replays fixture cases against an engine supplied by the caller, so this module compiles and is
 * testable with no JsonLogic engine implementation present.
 */
class FixtureReplay(private val evaluate: (rule: JsonElement, data: JsonElement) -> JsonElement) {

    fun runValues(cases: List<ValueFixtureCase>, supportedOps: Set<String>? = null): ReplayResult {
        var ran = 0
        var passed = 0
        val failures = mutableListOf<String>()
        for (case in cases) {
            if (supportedOps != null && !isSupported(case.rule, supportedOps)) continue
            ran++
            val outcome = runCatching { evaluate(case.rule, case.data) }
            val actual = outcome.getOrNull()
            if (actual != null && jsonSemanticEquals(actual, case.expected)) {
                passed++
            } else {
                val actualDescription = outcome.fold(
                    onSuccess = { it.toString() },
                    onFailure = { "threw ${it::class.simpleName}: ${it.message}" },
                )
                failures += "[#${case.index}] section=\"${case.section}\" rule=${case.rule} " +
                    "data=${case.data} expected=${case.expected} actual=$actualDescription"
            }
        }
        return ReplayResult(ran, passed, ran - passed, cases.size - ran, failures)
    }

    fun runErrors(
        cases: List<ErrorFixtureCase>,
        evaluateExpectingError: (rule: JsonElement, data: JsonElement) -> ErrorOutcome?,
        supportedOps: Set<String>? = null,
    ): ReplayResult {
        var ran = 0
        var passed = 0
        val failures = mutableListOf<String>()
        for (case in cases) {
            if (supportedOps != null && !isSupported(case.rule, supportedOps)) continue
            ran++
            val outcome = evaluateExpectingError(case.rule, case.data)
            val ok = outcome != null &&
                outcome.jsonPath == case.expectedJsonPath &&
                outcome.message == case.expectedMessage
            if (ok) {
                passed++
            } else {
                val actualDescription = outcome
                    ?.let { "path=\"${it.jsonPath}\" message=\"${it.message}\"" }
                    ?: "did not throw"
                failures += "[#${case.index}] rule=${case.rule} data=${case.data} " +
                    "expectedPath=\"${case.expectedJsonPath}\" expectedMessage=\"${case.expectedMessage}\" " +
                    "actual=$actualDescription"
            }
        }
        return ReplayResult(ran, passed, ran - passed, cases.size - ran, failures)
    }

    private fun isSupported(rule: JsonElement, supportedOps: Set<String>): Boolean {
        val ops = mutableSetOf<String>()
        collectOperators(rule, ops)
        return ops.all { it in supportedOps }
    }

    private fun collectOperators(element: JsonElement, into: MutableSet<String>) {
        when (element) {
            is JsonObject -> for ((key, value) in element) {
                if (key != "var") into.add(key)
                collectOperators(value, into)
            }
            is JsonArray -> element.forEach { collectOperators(it, into) }
            else -> Unit
        }
    }
}
