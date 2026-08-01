package co.branch.jsonlogic.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Every literal here is one where a platform's own `String.toDouble` disagrees with
 * `java.lang.Double.parseDouble` on at least one target, so each case fails somewhere if the parser
 * stops converting through [co.branch.jsonlogic.internal.parseJavaDouble].
 *
 * Results are compared as raw bits against a Kotlin `Double` literal, which the host compiler folds
 * and every target therefore agrees on. Comparing two literals through a rule would not work: both
 * operands reach the same converter, so `{"==": [-645.4596E-2, -6.454596]}` holds even where the
 * conversion is wrong.
 */
class JsonLogicParserNumberLiteralTest {

    private fun assertParsesToBits(expected: Double, literal: String) {
        val node = JsonLogicParser.parse(literal)

        assertEquals(expected.toRawBits(), (node as JsonLogicNumber).value.toRawBits(), literal)
    }

    @Test
    fun roundsToNearestAtEveryLength() {
        assertParsesToBits(-6.454596, "-645.4596E-2")
        assertParsesToBits(2.032174365297909E7, "20321743.652979088958052")
        assertParsesToBits(-1.4360803548316393E9, "-1436080354.831639175")
    }

    @Test
    fun roundsAtTheSubnormalBoundary() {
        assertParsesToBits(Double.fromBits(9), "4.4e-323")
        assertParsesToBits(Double.fromBits(0x2FFFFFFFFFFFFF), "8.900295434028805e-308")
        // Just under half the smallest subnormal, so it rounds to zero rather than up to it.
        assertParsesToBits(0.0, "2.4703282292062327e-324")
    }

    @Test
    fun overflowsToInfinity() {
        assertParsesToBits(Double.POSITIVE_INFINITY, "1.797693134862315808e308")
    }

    /** A digit far past the point any fixed-width buffer holds still breaks the tie upward. */
    @Test
    fun honoursSignificantDigitsAtAnyDistance() {
        val tie = "1.0000000000000001110223024625156540423631668090820312500"

        assertParsesToBits(1.0000000000000002, tie + "0".repeat(600) + "1")
    }

    @Test
    fun reportsANonNumericLiteralAsAParseFailure() {
        val exception = assertFailsWith<JsonLogicParseException> { JsonLogicParser.parse("[abc]") }

        assertEquals("$[0]", exception.jsonPath)
    }

    @Test
    fun locatesANonNumericLiteralInsideAnOperation() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("""{"+": [abc, 1]}""")
        }

        assertEquals("$.+[0]", exception.jsonPath)
    }
}
