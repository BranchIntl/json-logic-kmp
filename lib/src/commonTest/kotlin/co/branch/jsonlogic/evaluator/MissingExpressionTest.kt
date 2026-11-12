package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins `missing` and `missing_some`, including how they look each key up the way `var` does. */
class MissingExpressionTest {

    private val apple = mapOf("a" to "apple", "c" to "carrot")

    @Test
    fun missingReportsTheKeysTheDataDoesNotProvide() {
        assertEquals(listOf("b"), evaluate("""{"missing": ["a", "b"]}""", apple))
        assertEquals(emptyList<Any?>(), evaluate("""{"missing": ["a", "c"]}""", apple))
        assertEquals(listOf("a", "b"), evaluate("""{"missing": ["a", "b"]}""", emptyMap<String, Any?>()))
    }

    @Test
    fun aSoleScalarArgumentIsOneKey() {
        assertEquals(listOf("a"), evaluate("""{"missing": "a"}""", emptyMap<String, Any?>()))
        assertEquals(emptyList<Any?>(), evaluate("""{"missing": "a"}""", apple))
    }

    @Test
    fun repeatedKeysAreReportedOncePerRepeatInTheOrderAsked() {
        assertEquals(listOf("b", "a", "b"), evaluate("""{"missing": ["b", "a", "b"]}""", emptyMap<String, Any?>()))
    }

    @Test
    fun dottedKeysWalkNestedMaps() {
        assertEquals(emptyList<Any?>(), evaluate("""{"missing": ["a.b"]}""", mapOf("a" to mapOf("b" to 1))))
        assertEquals(listOf("a.c"), evaluate("""{"missing": ["a.b", "a.c"]}""", mapOf("a" to mapOf("b" to 1))))
        assertEquals(listOf("a.b.c"), evaluate("""{"missing": ["a.b.c"]}""", mapOf("a" to mapOf("b" to 1))))
    }

    @Test
    fun aKeyHoldingAMapIsPresentEvenAnEmptyOne() {
        assertEquals(emptyList<Any?>(), evaluate("""{"missing": ["response"]}""", mapOf("response" to mapOf("status" to "ok"))))
        assertEquals(emptyList<Any?>(), evaluate("""{"missing": ["a"]}""", mapOf("a" to emptyMap<String, Any?>())))
    }

    @Test
    fun aKeyWithALiteralDotIsSplitLikeVar() {
        assertEquals(listOf("a.b"), evaluate("""{"missing": ["a.b"]}""", mapOf("a.b" to 1)))
    }

    @Test
    fun theEmptyKeyIsTheWholeData() {
        assertEquals(emptyList<Any?>(), evaluate("""{"missing": [""]}""", mapOf("a" to 1)))
        assertEquals(listOf(""), evaluate("""{"missing": [""]}""", null))
    }

    @Test
    fun aKeyHoldingNullOrTheEmptyStringIsMissing() {
        val data = mapOf("a" to null, "b" to "", "c" to 0, "d" to false, "e" to emptyList<Any?>())

        assertEquals(listOf("a", "b"), evaluate("""{"missing": ["a", "b", "c", "d", "e"]}""", data))
    }

    @Test
    fun aNestedLeafHoldingNullOrTheEmptyStringIsMissing() {
        val data = mapOf("a" to mapOf("b" to null, "c" to "", "d" to "x"))

        assertEquals(listOf("a.b", "a.c"), evaluate("""{"missing": ["a.b", "a.c", "a.d"]}""", data))
    }

    @Test
    fun missingSomeCountsANullOrEmptyKeyAsMissing() {
        val data = mapOf("a" to null, "b" to "", "c" to 1)

        assertEquals(listOf("a", "b"), evaluate("""{"missing_some": [2, ["a", "b", "c"]]}""", data))
        assertEquals(emptyList<Any?>(), evaluate("""{"missing_some": [1, ["a", "b", "c"]]}""", data))
    }

    @Test
    fun anIndexSegmentReachesIntoAList() {
        val data = mapOf("items" to listOf(1, null))

        assertEquals(emptyList<Any?>(), evaluate("""{"missing": ["items", "items.0"]}""", data))
        assertEquals(listOf("items.1", "items.2", "items.-1"), evaluate("""{"missing": ["items.1", "items.2", "items.-1"]}""", data))
    }

    @Test
    fun aKeyVarWouldThrowOnIsMissingInstead() {
        val data = mapOf("a" to listOf(1, 2), "true" to 1)

        assertEquals(listOf("a.x"), evaluate("""{"missing": ["a.x"]}""", data))
        assertEquals(listOf(true, listOf("a")), evaluate("""{"missing": [true, ["a"]]}""", data))
    }

    @Test
    fun aNumberKeyIndexesListDataOnly() {
        assertEquals(listOf(2.0), evaluate("""{"missing": [0, 2]}""", listOf("x", "y")))
        assertEquals(emptyList<Any?>(), evaluate("""{"missing": ["1"]}""", listOf("x", "y")))
        assertEquals(listOf(0.0), evaluate("""{"missing": [0]}""", mapOf("0" to 1)))
    }

    @Test
    fun withoutMapOrListDataEveryKeyIsMissing() {
        assertEquals(listOf("a", "b"), evaluate("""{"missing": ["a", "b"]}""", null))
        assertEquals(listOf("0", "length"), evaluate("""{"missing": ["0", "length"]}""", "not a map"))
    }

    @Test
    fun missingSomeIsSatisfiedOnceEnoughKeysArePresent() {
        assertEquals(emptyList<Any?>(), evaluate("""{"missing_some": [1, ["a", "b"]]}""", apple))
        assertEquals(
            emptyList<Any?>(),
            evaluate("""{"missing_some": [2, ["a", "b", "c"]]}""", mapOf("a" to 1, "b" to 2, "c" to 3)),
        )
    }

    @Test
    fun missingSomeReportsEveryAbsentKeyBelowTheThreshold() {
        assertEquals(listOf("b", "c"), evaluate("""{"missing_some": [2, ["a", "b", "c"]]}""", mapOf("a" to "apple")))
        assertEquals(listOf("a", "b"), evaluate("""{"missing_some": [1, ["a", "b"]]}""", mapOf("d" to 1)))
    }

    @Test
    fun missingSomeCountsEachRepeatAsMissing() {
        assertEquals(listOf("a", "a"), evaluate("""{"missing_some": [2, ["a", "a", "b"]]}""", mapOf("b" to 1)))
    }

    @Test
    fun missingSomeWithoutMapDataReportsEveryKey() {
        assertEquals(listOf("a", "b", "c"), evaluate("""{"missing_some": [2, ["a", "b", "c"]]}""", null))
    }

    @Test
    fun aThresholdOfZeroIsAlwaysSatisfied() {
        assertEquals(emptyList<Any?>(), evaluate("""{"missing_some": [0, ["a", "b", "c"]]}""", null))
        assertEquals(emptyList<Any?>(), evaluate("""{"missing_some": [0, ["a", "b", "c"]]}""", emptyMap<String, Any?>()))
    }

    @Test
    fun missingSomeRequiresANumberAndAnArray() {
        for (rule in listOf("""{"missing_some": [1]}""", """{"missing_some": [1, 2]}""", """{"missing_some": ["1", ["a"]]}""")) {
            val exception = assertFailsWith<JsonLogicEvaluationException>(rule) {
                evaluate(rule, emptyMap<String, Any?>())
            }

            assertEquals(
                "missing_some expects first argument to be an integer and the second argument to be an array",
                exception.message,
            )
            assertEquals("$.missing_some", exception.jsonPath)
        }
    }

    @Test
    fun missingSomeRequiresTheThresholdToBeADouble() {
        // Every number the evaluator produces is a Double, so the check is a Double test rather than
        // a Number test: an operation handing over an Int does not satisfy it.
        val expressions = missingExpressions + ConstantExpression("intOne", 1)

        assertFailsWith<JsonLogicEvaluationException> {
            evaluate("""{"missing_some": [{"intOne": []}, ["a"]]}""", emptyMap<String, Any?>(), expressions)
        }
    }
}
