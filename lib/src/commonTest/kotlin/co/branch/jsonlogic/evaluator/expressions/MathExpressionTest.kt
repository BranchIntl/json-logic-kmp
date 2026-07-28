package co.branch.jsonlogic.evaluator.expressions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Pins the arithmetic operations, including how they read lists, strings and no argument at all. */
class MathExpressionTest {

    @Test
    fun additionAndMultiplicationFoldEveryArgument() {
        assertEquals(15.0, evaluateRule("""{"+": [1, 2, 3, 4, 5]}"""))
        assertEquals(120.0, evaluateRule("""{"*": [1, 2, 3, 4, 5]}"""))
        assertEquals(1.0, evaluateRule("""{"min": [3, 2, 1]}"""))
        assertEquals(3.0, evaluateRule("""{"max": [1, 2, 3]}"""))
    }

    @Test
    fun subtractionDivisionAndModuloStopAfterTwoArguments() {
        assertEquals(-1.0, evaluateRule("""{"-": [1, 2, 3]}"""))
        assertEquals(0.5, evaluateRule("""{"/": [1, 2, 3]}"""))
        assertEquals(1.0, evaluateRule("""{"%": [10, 3, 2]}"""))
    }

    @Test
    fun aSoleArgumentIsNegatedBySubtractionAndRejectedByDivision() {
        assertEquals(-3.0, evaluateRule("""{"-": [3]}"""))
        assertEquals(-5.0, evaluateRule("""{"-": ["5"]}"""))
        assertNull(evaluateRule("""{"/": [1]}"""))
        assertEquals(1.0, evaluateRule("""{"%": [1]}"""))
        assertEquals(1.0, evaluateRule("""{"max": [1]}"""))
        assertEquals(1.0, evaluateRule("""{"+": [1]}"""))
    }

    @Test
    fun noArgumentAtAllIsNull() {
        assertNull(evaluateRule("""{"+": []}"""))
        assertNull(evaluateRule("""{"*": []}"""))
        assertNull(evaluateRule("""{"min": []}"""))
        assertNull(evaluateRule("""{"-": []}"""))
    }

    @Test
    fun aSoleListArgumentIsSpreadIntoArguments() {
        assertEquals(6.0, evaluateRule("""{"+": [[1, 2, 3]]}"""))
        assertEquals(24.0, evaluateRule("""{"*": [[2, 3, 4]]}"""))
        assertEquals(-5.0, evaluateRule("""{"-": [[5]]}"""))
        assertEquals(-1.0, evaluateRule("""{"-": [[1, 2, 3]]}"""))
        assertEquals(1.0, evaluateRule("""{"min": [[1, 2, 3]]}"""))
    }

    @Test
    fun additionAndMultiplicationTakeAListArgumentsFirstElement() {
        assertEquals(3.0, evaluateRule("""{"+": [1, [2, 3]]}"""))
        assertEquals(6.0, evaluateRule("""{"*": [[3, 4], [2, 3]]}"""))
        // The spread leaves two list arguments, each of which unwraps down to its first number.
        assertEquals(4.0, evaluateRule("""{"+": [[[1, 2], [3, 4]]]}"""))
        assertEquals(6.0, evaluateRule("""{"+": [[[[5]]], 1]}"""))
    }

    @Test
    fun anEmptyListArgumentHasNoFirstElementAndIsNull() {
        assertNull(evaluateRule("""{"+": [[]]}"""))
        assertNull(evaluateRule("""{"+": [1, []]}"""))
        assertNull(evaluateRule("""{"*": [3, []]}"""))
        assertNull(evaluateRule("""{"*": [[], []]}"""))
        assertNull(evaluateRule("""{"min": [[]]}"""))
    }

    @Test
    fun onlyAdditionAndMultiplicationReadListArguments() {
        assertNull(evaluateRule("""{"min": [3, [1, 2]]}"""))
        assertNull(evaluateRule("""{"-": [3, [1, 2]]}"""))
        assertNull(evaluateRule("""{"min": [3, {"var": "a"}]}""", mapOf("a" to listOf(1, 2))))
    }

    @Test
    fun stringArgumentsAreReadAsJavaWouldParseThem() {
        assertEquals(2.0, evaluateRule("""{"+": [" 1 ", 1]}"""))
        assertEquals(2.0, evaluateRule("""{"+": ["1d", 1]}"""))
        assertEquals(17.0, evaluateRule("""{"+": ["0x10p0", 1]}"""))
        assertEquals(6.0, evaluateRule("""{"*": [2, "3"]}"""))
        assertEquals(2.0, evaluateRule("""{"min": [3, "2"]}"""))
        assertEquals(Double.NaN, evaluateRule("""{"+": ["NaN", 1]}"""))
        assertEquals(Double.POSITIVE_INFINITY, evaluateRule("""{"+": ["Infinity", 1]}"""))
        assertEquals((-0.0).toRawBits(), evaluateToRawBits("""{"+": ["-0"]}"""))
    }

    @Test
    fun anArgumentNoNumberCanBeReadOutOfIsNull() {
        assertNull(evaluateRule("""{"+": ["1x", 1]}"""))
        assertNull(evaluateRule("""{"+": ["", 1]}"""))
        assertNull(evaluateRule("""{"+": ["0x10", 1]}"""))
        assertNull(evaluateRule("""{"+": [true, 1]}"""))
        assertNull(evaluateRule("""{"+": [null, 1]}"""))
        assertNull(evaluateRule("""{"-": [1, "two"]}"""))
        assertNull(evaluateRule("""{"+": [{"var": "a"}, 1]}""", mapOf("a" to mapOf("b" to 1))))
    }

    @Test
    fun divisionAndModuloByZeroFollowIeee() {
        assertEquals(Double.POSITIVE_INFINITY, evaluateRule("""{"/": [1, 0]}"""))
        assertEquals(Double.NEGATIVE_INFINITY, evaluateRule("""{"/": [-1, 0]}"""))
        assertEquals(Double.NaN, evaluateRule("""{"/": [0, 0]}"""))
        assertEquals(Double.NaN, evaluateRule("""{"%": [1, 0]}"""))
        assertEquals(-1.0, evaluateRule("""{"%": [-7, 3]}"""))
    }

    @Test
    fun minAndMaxCarryNaNThroughFromEitherSide() {
        assertEquals(Double.NaN, evaluateRule("""{"min": [1, {"/": [0, 0]}]}"""))
        assertEquals(Double.NaN, evaluateRule("""{"min": [{"/": [0, 0]}, 1]}"""))
        assertEquals(Double.NaN, evaluateRule("""{"max": [1, {"/": [0, 0]}]}"""))
        assertEquals(Double.NaN, evaluateRule("""{"max": [{"/": [0, 0]}, 1]}"""))
        assertEquals(Double.NaN, evaluateRule("""{"min": [1, {"/": [0, 0]}, 0]}"""))
    }

    @Test
    fun minAndMaxOrderNegativeZeroBelowZero() {
        assertEquals((-0.0).toRawBits(), evaluateToRawBits("""{"min": [0, {"-": [0]}]}"""))
        assertEquals((-0.0).toRawBits(), evaluateToRawBits("""{"min": [{"-": [0]}, 0]}"""))
        assertEquals(0.0.toRawBits(), evaluateToRawBits("""{"max": [0, {"-": [0]}]}"""))
        assertEquals(0.0.toRawBits(), evaluateToRawBits("""{"max": [{"-": [0]}, 0]}"""))
    }

    @Test
    fun negatingZeroSignsIt() {
        assertEquals((-0.0).toRawBits(), evaluateToRawBits("""{"-": [0]}"""))
    }
}
