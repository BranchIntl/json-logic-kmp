package co.branch.jsonlogic.playground.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import co.branch.jsonlogic.playground.Operation
import co.branch.jsonlogic.playground.OperationGroups
import co.branch.jsonlogic.playground.theme.LocalMonospaceStyle
import co.branch.jsonlogic.playground.theme.LocalPlaygroundColors

/**
 * The full operator list, collapsed by default so it never competes with the editors for space.
 *
 * Clicking an entry inserts its snippet, which is why [onInsert] takes the snippet rather than the
 * whole operation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperationsReference(
    expanded: Boolean,
    onToggle: () -> Unit,
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPlaygroundColors.current

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 5.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Chevron(
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 90f else 0f),
            )
            Text(
                text = "OPERATIONS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Bounded so an expanded reference scrolls itself instead of squeezing the
                    // editors out of the viewport.
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OperationGroups.forEach { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.gutter,
                            modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            group.operations.forEach { operation ->
                                OperationRow(operation, onInsert)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationRow(operation: Operation, onInsert: (String) -> Unit) {
    val colors = LocalPlaygroundColors.current

    Row(
        modifier = Modifier
            .width(348.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onInsert(operation.snippet) }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The column is fixed so every summary starts at the same offset; the pill inside hugs its
        // text, because a lone "+" centred in a wide filled box reads as a rendering mistake.
        Box(Modifier.width(112.dp)) {
            Text(
                text = operation.symbol,
                style = LocalMonospaceStyle.current,
                color = colors.syntax.key,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.chip)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Text(
            text = operation.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

/** A right-pointing chevron; the caller rotates it to mark the expanded state. */
@Composable
private fun Chevron(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(10.dp)) {
        val stroke = size.minDimension * 0.16f
        val tip = Offset(size.width * 0.72f, size.height / 2f)
        drawLine(tint, Offset(size.width * 0.34f, size.height * 0.2f), tip, stroke, StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.34f, size.height * 0.8f), tip, stroke, StrokeCap.Round)
    }
}
