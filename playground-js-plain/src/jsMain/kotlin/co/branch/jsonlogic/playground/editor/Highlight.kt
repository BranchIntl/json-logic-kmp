package co.branch.jsonlogic.playground.editor

/** A run of characters painted as one unit. A null [kind] is a gap between two tokens. */
internal data class HighlightSpan(val kind: JsonTokenKind?, val text: String)

/**
 * Cuts [text] into painted runs. Whitespace produces no token, so the gaps between tokens come back
 * as spans of their own and every character of the input appears exactly once, in order.
 */
internal fun highlightSpans(text: String): List<HighlightSpan> {
    val spans = mutableListOf<HighlightSpan>()
    var cursor = 0

    JsonTokenizer.tokenize(text).forEach { token ->
        if (token.start > cursor) {
            spans.add(HighlightSpan(null, text.substring(cursor, token.start)))
        }
        spans.add(HighlightSpan(token.kind, text.substring(token.start, token.endExclusive)))
        cursor = token.endExclusive
    }

    if (cursor < text.length) spans.add(HighlightSpan(null, text.substring(cursor)))

    return spans
}

/**
 * Regroups [highlightSpans] into one list per logical line, cutting a span's own text at each
 * newline. A token really does run across lines: an unterminated string covers the rest of the
 * input, and that is the state of the buffer for as long as someone is typing one.
 *
 * There is always one more line than [text] has newlines, so a trailing newline produces a final
 * empty line, the way a textarea shows one.
 */
internal fun highlightLines(text: String): List<List<HighlightSpan>> {
    val lines = mutableListOf(mutableListOf<HighlightSpan>())

    highlightSpans(text).forEach { span ->
        span.text.split('\n').forEachIndexed { index, piece ->
            if (index > 0) lines.add(mutableListOf())
            if (piece.isNotEmpty()) lines.last().add(HighlightSpan(span.kind, piece))
        }
    }

    return lines
}
