package co.branch.jsonlogic.playground.editor

/** The token classes the editor colours. */
enum class JsonTokenKind {
    /** A string immediately followed by `:`. */
    Key,
    StringValue,
    Number,
    /** `true`, `false` or `null`. */
    Literal,
    Punctuation,
    /** Anything the scanner could not classify, including an unterminated string. */
    Unknown,
}

/** Half-open range `[start, endExclusive)` of [JsonTokenizer.tokenize]'s input. */
data class JsonToken(val start: Int, val endExclusive: Int, val kind: JsonTokenKind)

/**
 * A scanner, not a parser: the editors call it on every keystroke, so it never throws and never
 * rejects. Anything it cannot classify becomes [JsonTokenKind.Unknown], and whitespace produces no
 * token at all, so the spans need not cover the whole input.
 */
object JsonTokenizer {

    private const val PUNCTUATION = "{}[],:"

    fun tokenize(text: String): List<JsonToken> {
        val tokens = mutableListOf<JsonToken>()
        var index = 0

        while (index < text.length) {
            val char = text[index]

            index = when {
                char.isWhitespace() -> index + 1
                char == '"' -> readString(text, index, tokens)
                char == '-' || char.isDigit() -> readNumber(text, index, tokens)
                char.isLetter() -> readWord(text, index, tokens)
                char in PUNCTUATION -> {
                    tokens.add(JsonToken(index, index + 1, JsonTokenKind.Punctuation))
                    index + 1
                }

                else -> {
                    tokens.add(JsonToken(index, index + 1, JsonTokenKind.Unknown))
                    index + 1
                }
            }
        }

        return tokens
    }

    /** Reads a quoted string starting at [start], and returns the index just past it. */
    private fun readString(text: String, start: Int, tokens: MutableList<JsonToken>): Int {
        var index = start + 1
        var terminated = false

        while (index < text.length) {
            when (text[index]) {
                '\\' -> index++ // Skip whatever follows, so an escaped quote does not end the string.
                '"' -> {
                    terminated = true
                    index++
                    break
                }
            }
            if (!terminated) index++
        }

        val end = minOf(index, text.length)
        val kind = when {
            !terminated -> JsonTokenKind.Unknown
            isFollowedByColon(text, end) -> JsonTokenKind.Key
            else -> JsonTokenKind.StringValue
        }
        tokens.add(JsonToken(start, end, kind))

        return end
    }

    private fun isFollowedByColon(text: String, from: Int): Boolean {
        var index = from
        while (index < text.length && text[index].isWhitespace()) index++

        return index < text.length && text[index] == ':'
    }

    private fun readNumber(text: String, start: Int, tokens: MutableList<JsonToken>): Int {
        var index = start + 1
        while (index < text.length && (text[index].isDigit() || text[index] in ".eE+-")) index++
        tokens.add(JsonToken(start, index, JsonTokenKind.Number))

        return index
    }

    private fun readWord(text: String, start: Int, tokens: MutableList<JsonToken>): Int {
        var index = start
        while (index < text.length && text[index].isLetter()) index++

        val kind = when (text.substring(start, index)) {
            "true", "false", "null" -> JsonTokenKind.Literal
            else -> JsonTokenKind.Unknown
        }
        tokens.add(JsonToken(start, index, kind))

        return index
    }
}
