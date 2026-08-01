package co.branch.jsonlogic.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class JsonLogicParserMalformedInputTest {
    @Test
    fun unparseableJsonThrowsParseExceptionAtRootPath() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("{not valid json")
        }

        assertEquals("$", exception.jsonPath)
        assertNotNull(exception.cause)
    }

    @Test
    fun emptyInputThrowsParseExceptionAtRootPath() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("")
        }

        assertEquals("$", exception.jsonPath)
        assertNotNull(exception.cause)
    }

    @Test
    fun truncatedArrayThrowsParseExceptionAtRootPath() {
        val exception = assertFailsWith<JsonLogicParseException> {
            JsonLogicParser.parse("[1, 2")
        }

        assertEquals("$", exception.jsonPath)
        assertNotNull(exception.cause)
    }
}
