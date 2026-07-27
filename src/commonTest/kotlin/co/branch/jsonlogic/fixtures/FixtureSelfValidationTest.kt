package co.branch.jsonlogic.fixtures

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Validates the embedded fixture data and the semantic comparator on every KMP target, with no
 * JsonLogic engine involved. This is the cross-target proof that the codegen and fixture library
 * work identically on jvm, Android, iOS, and wasmJs.
 */
class FixtureSelfValidationTest {

    @Test
    fun embeddedFixtureFilesAreEmbedded() {
        assertTrue(EmbeddedFixtures.files.getValue("fixtures.json").isNotBlank())
        assertTrue(EmbeddedFixtures.files.getValue("error-fixtures.json").isNotBlank())
    }

    @Test
    fun valueFixtureCaseCountIs289() {
        assertEquals(289, FixtureLoader.loadValueFixtures().size)
    }

    @Test
    fun sectionHeaderCountIs17() {
        assertEquals(17, FixtureLoader.countSectionHeaders())
    }

    @Test
    fun errorFixtureCaseCountIs46() {
        assertEquals(46, FixtureLoader.loadErrorFixtures().size)
    }

    @Test
    fun everyRuleDataExpectedParses() {
        val valueCases = FixtureLoader.loadValueFixtures()
        assertEquals(289, valueCases.size)
        for (case in valueCases) {
            // Loading already parsed rule/data/expected into JsonElement; toString() here just
            // touches each one so a malformed element would surface as a test failure.
            case.rule.toString()
            case.data.toString()
            case.expected.toString()
        }
        val errorCases = FixtureLoader.loadErrorFixtures()
        assertEquals(46, errorCases.size)
        for (case in errorCases) {
            case.rule.toString()
            case.data.toString()
        }
    }

    @Test
    fun semanticEquals_integerAndDoubleAreEqual() {
        assertTrue(jsonSemanticEquals(JsonPrimitive(1), JsonPrimitive(1.0)))
    }

    @Test
    fun semanticEquals_stringAndNumberAreNeverEqual() {
        assertFalse(jsonSemanticEquals(JsonPrimitive("1"), JsonPrimitive(1)))
    }

    @Test
    fun semanticEquals_equalStringsAreEqual() {
        assertTrue(jsonSemanticEquals(JsonPrimitive("a"), JsonPrimitive("a")))
    }

    @Test
    fun semanticEquals_equalBooleansAreEqual() {
        assertTrue(jsonSemanticEquals(JsonPrimitive(true), JsonPrimitive(true)))
    }

    @Test
    fun semanticEquals_nestedArraysAndObjectsRecurse() {
        val a = buildJsonObject {
            put("list", buildJsonArray {
                add(JsonPrimitive(1))
                add(JsonPrimitive("x"))
            })
        }
        val b = buildJsonObject {
            put("list", buildJsonArray {
                add(JsonPrimitive(1.0))
                add(JsonPrimitive("x"))
            })
        }
        assertTrue(jsonSemanticEquals(a, b))
    }

    @Test
    fun semanticEquals_emptyArraysAreEqual() {
        assertTrue(jsonSemanticEquals(buildJsonArray {}, buildJsonArray {}))
    }

    @Test
    fun semanticEquals_emptyObjectsAreEqual() {
        assertTrue(jsonSemanticEquals(buildJsonObject {}, buildJsonObject {}))
    }
}
