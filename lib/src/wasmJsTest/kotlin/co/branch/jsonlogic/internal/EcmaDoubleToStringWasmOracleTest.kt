package co.branch.jsonlogic.internal

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks [ecmaDoubleToString] against the authority it reimplements: the JavaScript runtime hosting
 * this target, whose `String(number)` *is* ECMAScript's `Number::toString`.
 *
 * The corresponding check for [canonicalDoubleToString] lives in `jvmTest` against the running JDK.
 * Between them, both renderings this engine produces are pinned to a real implementation of the
 * specification they claim to follow, over the same shared corpus every target walks.
 */
class EcmaDoubleToStringWasmOracleTest {

    @Test
    fun formattingMatchesThisJavaScriptRuntime() {
        val values = canonicalNumberCorpus(randomCount = 20_000)
        var checked = 0
        val failures = StringBuilder()
        var failed = 0

        for (value in values) {
            val mine = ecmaDoubleToString(value)
            val theirs = jsNumberToString(value)
            checked++
            if (mine != theirs) {
                failed++
                if (failed <= 10) {
                    failures.append("\n  bits=0x${value.toRawBits().toString(16)}: mine=$mine js=$theirs")
                }
            }
        }

        assertTrue(checked > 5_000, "corpus was unexpectedly small: $checked values")
        assertEquals(0, failed, "$failed of $checked values disagree with this runtime:$failures")
    }

    @Test
    fun specialValuesMatchThisJavaScriptRuntime() {
        assertEquals(jsNumberToString(Double.NaN), ecmaDoubleToString(Double.NaN))
        assertEquals(jsNumberToString(Double.POSITIVE_INFINITY), ecmaDoubleToString(Double.POSITIVE_INFINITY))
        assertEquals(jsNumberToString(Double.NEGATIVE_INFINITY), ecmaDoubleToString(Double.NEGATIVE_INFINITY))
        assertEquals(jsNumberToString(0.0), ecmaDoubleToString(0.0))
        assertEquals(jsNumberToString(-0.0), ecmaDoubleToString(-0.0))
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsNumberToString(value: Double): String = js("String(value)")
