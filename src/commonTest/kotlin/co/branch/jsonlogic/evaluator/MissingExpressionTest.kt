package co.branch.jsonlogic.evaluator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pins `missing` and `missing_some`, including how they flatten the data's keys. */
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
    fun repeatedKeysAreReportedOnceInTheOrderAsked() {
        assertEquals(listOf("b", "a"), evaluate("""{"missing": ["b", "a", "b"]}""", emptyMap<String, Any?>()))
    }

    @Test
    fun keysAreMatchedAgainstFlattenedPaths() {
        assertEquals(emptyList<Any?>(), evaluate("""{"missing": ["a.b"]}""", mapOf("a" to mapOf("b" to 1))))
        assertEquals(listOf("a.c"), evaluate("""{"missing": ["a.b", "a.c"]}""", mapOf("a" to mapOf("b" to 1))))
        assertEquals(listOf("a"), evaluate("""{"missing": ["a"]}""", mapOf("a" to mapOf("b" to 1))))
    }

    @Test
    fun anEmptyNestedMapProvidesNoKeyAtAll() {
        assertEquals(listOf("a"), evaluate("""{"missing": ["a"]}""", mapOf("a" to emptyMap<String, Any?>())))
    }

    @Test
    fun flatteningDoesNotReachIntoLists() {
        assertEquals(listOf("a.0"), evaluate("""{"missing": ["a.0"]}""", mapOf("a" to listOf(1, 2))))
        assertEquals(emptyList<Any?>(), evaluate("""{"missing": ["a"]}""", mapOf("a" to listOf(1, 2))))
    }

    @Test
    fun withoutMapDataEveryKeyIsMissing() {
        assertEquals(listOf("a", "b"), evaluate("""{"missing": ["a", "b"]}""", null))
        assertEquals(listOf("a", "b"), evaluate("""{"missing": ["a", "b"]}""", "not a map"))
        assertEquals(listOf("a", "b"), evaluate("""{"missing": ["a", "b"]}""", listOf("a", "b")))
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
