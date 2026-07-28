package co.branch.jsonlogic.playground.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonTokenizerTest {

    @Test
    fun classifiesKeysApartFromStringValues() {
        val tokens = JsonTokenizer.tokenize("""{"a": "b"}""")

        assertEquals(
            listOf(
                JsonTokenKind.Punctuation,
                JsonTokenKind.Key,
                JsonTokenKind.Punctuation,
                JsonTokenKind.StringValue,
                JsonTokenKind.Punctuation,
            ),
            tokens.map { it.kind },
        )
    }

    @Test
    fun aKeyIsStillAKeyAcrossWhitespace() {
        val tokens = JsonTokenizer.tokenize("{\"a\"\n  : 1}")

        assertEquals(JsonTokenKind.Key, tokens[1].kind)
    }

    @Test
    fun classifiesLiteralsAndNumbers() {
        val kinds = JsonTokenizer.tokenize("[true, false, null, -1.5e3]").map { it.kind }

        assertEquals(
            listOf(JsonTokenKind.Literal, JsonTokenKind.Literal, JsonTokenKind.Literal, JsonTokenKind.Number),
            kinds.filter { it != JsonTokenKind.Punctuation },
        )
    }

    @Test
    fun escapedQuoteDoesNotTerminateAString() {
        val tokens = JsonTokenizer.tokenize("""["a\"b"]""")

        assertEquals(1, tokens.count { it.kind == JsonTokenKind.StringValue })
    }

    /**
     * The editors tokenize on every keystroke, so half-typed text is the common case, not the edge
     * case. Each of these is a real intermediate state of typing valid JSON.
     */
    @Test
    fun survivesPartialInput() {
        val partials = listOf(
            "",
            "{",
            """{"a""",
            """{"a"”""",
            """{"a": tru""",
            """{"a": 1e""",
            "\\",
            """["a\""",
            "{\"a\": \"unterminated",
            "@#$%",
        )

        partials.forEach { partial ->
            val tokens = JsonTokenizer.tokenize(partial)

            // Ranges must stay inside the input, or building the AnnotatedString throws.
            tokens.forEach { token ->
                assertTrue(
                    token.start in 0..partial.length && token.endExclusive in token.start..partial.length,
                    "token $token out of bounds for \"$partial\"",
                )
            }
            assertEquals(
                tokens.sortedBy { it.start },
                tokens,
                "tokens for \"$partial\" must be in order",
            )
        }
    }

    @Test
    fun unterminatedStringIsMarkedInvalid() {
        val tokens = JsonTokenizer.tokenize("""{"a": "oops""")

        assertEquals(JsonTokenKind.Unknown, tokens.last().kind)
    }
}
