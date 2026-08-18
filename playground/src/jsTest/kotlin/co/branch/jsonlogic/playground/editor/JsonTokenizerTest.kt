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

    @Test
    fun survivesPartialInput() {
        PartialInputs.forEach { partial ->
            val tokens = JsonTokenizer.tokenize(partial)

            // Every range is cut out of the input with substring, which throws on one that runs
            // past the end.
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

/** Tokenizing on every keystroke makes half-typed text the common case, not the edge case. */
internal val PartialInputs = listOf(
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
