package co.branch.jsonlogic.playground.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import co.branch.jsonlogic.playground.theme.LocalPlaygroundColors
import co.branch.jsonlogic.playground.theme.SyntaxColors

/** Colours [text] token by token, leaving the characters themselves untouched. */
fun highlightJson(text: String, colors: SyntaxColors): AnnotatedString = buildAnnotatedString {
    var cursor = 0

    JsonTokenizer.tokenize(text).forEach { token ->
        // Whitespace produces no token, so the gaps between them are appended unstyled.
        if (token.start > cursor) {
            append(text.substring(cursor, token.start))
        }
        withStyle(SpanStyle(color = colors.of(token.kind))) {
            append(text.substring(token.start, token.endExclusive))
        }
        cursor = token.endExclusive
    }

    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}

private fun SyntaxColors.of(kind: JsonTokenKind) = when (kind) {
    JsonTokenKind.Key -> key
    JsonTokenKind.StringValue -> string
    JsonTokenKind.Number -> number
    JsonTokenKind.Literal -> literal
    JsonTokenKind.Punctuation -> punctuation
    JsonTokenKind.Unknown -> invalid
}

/**
 * [OffsetMapping.Identity] is safe because the transformation only adds styles: every character
 * keeps its position, so the cursor and selection need no translation.
 */
@Composable
fun rememberJsonHighlight(): VisualTransformation {
    val colors = LocalPlaygroundColors.current.syntax

    return remember(colors) {
        VisualTransformation { text ->
            TransformedText(highlightJson(text.text, colors), OffsetMapping.Identity)
        }
    }
}
