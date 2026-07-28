package co.branch.jsonlogic.playground.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import co.branch.jsonlogic.playground.theme.LocalPlaygroundColors
import co.branch.jsonlogic.playground.theme.LocalMonospaceStyle

@Composable
fun JsonEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = rememberJsonHighlight(),
) {
    val colors = LocalPlaygroundColors.current
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Row(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 10.dp),
    ) {
        LineGutter(text = value.text, layout = layout, color = colors.gutter)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalMonospaceStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = visualTransformation,
            onTextLayout = { layout = it },
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
    }
}

/**
 * One text block in the editor's own style, so the two stay aligned for free — including across a
 * soft-wrapped line, which contributes blank gutter rows.
 */
@Composable
private fun LineGutter(text: String, layout: TextLayoutResult?, color: Color) {
    val numbers = remember(text, layout) { gutterText(text, layout) }

    Text(
        text = numbers,
        style = LocalMonospaceStyle.current,
        color = color,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
    )
}

private fun gutterText(text: String, layout: TextLayoutResult?): String {
    val lines = text.split('\n')
    val width = lines.size.toString().length
    // Right-alignment comes from the monospace font, so padding the numbers is enough.
    val numbers = lines.indices.map { (it + 1).toString().padStart(width) }

    if (layout == null) {
        return numbers.joinToString("\n")
    }

    // The layout can be a frame behind the text it is measured against, so every offset is clamped.
    val laidOut = layout.layoutInput.text.length
    var offset = 0

    return buildString {
        lines.forEachIndexed { index, line ->
            val first = layout.getLineForOffset(offset.coerceIn(0, laidOut))
            val last = layout.getLineForOffset((offset + line.length).coerceIn(0, laidOut))

            append(numbers[index])
            repeat((last - first).coerceAtLeast(0)) { append('\n') }
            if (index < lines.lastIndex) append('\n')

            offset += line.length + 1
        }
    }
}
