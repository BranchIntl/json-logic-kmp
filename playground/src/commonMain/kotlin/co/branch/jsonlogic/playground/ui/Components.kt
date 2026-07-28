package co.branch.jsonlogic.playground.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.branch.jsonlogic.playground.theme.LocalPlaygroundColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * A labelled, bordered region — the repeating unit of the layout.
 *
 * The label row sits outside the border so the content area is entirely the caller's, which is what
 * lets an editor fill it edge to edge.
 */
@Composable
fun Panel(
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colors = LocalPlaygroundColors.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(Modifier.weight(1f))
            trailing()
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.panel)
                .border(BorderStroke(1.dp, colors.panelBorder), RoundedCornerShape(10.dp)),
        ) {
            content()
        }
    }
}

/** A coloured dot plus a short caption, used to report whether a panel's JSON currently parses. */
@Composable
fun StatusLabel(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A rounded, tappable pill. Used for the example presets and the operator rows. */
@Composable
fun Chip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val colors = LocalPlaygroundColors.current
    val background = if (selected) MaterialTheme.colorScheme.primary else colors.chip
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = foreground,
            maxLines = 1,
        )
    }
}

/**
 * Sun and moon glyphs, drawn rather than pulled from an icon font.
 *
 * The two shapes are the only icons the playground needs, and drawing them keeps the bundle free of
 * an icon dependency.
 */
@Composable
fun SunIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(16.dp)) {
        val core = size.minDimension * 0.24f
        drawCircle(color = tint, radius = core, center = center)
        repeat(8) { index ->
            val angle = index * kotlin.math.PI.toFloat() / 4f
            val direction = Offset(cos(angle), sin(angle))
            drawLine(
                color = tint,
                start = center + direction * (core * 1.7f),
                end = center + direction * (core * 2.4f),
                strokeWidth = size.minDimension * 0.085f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun MoonIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(16.dp)
            // The crescent is carved out with BlendMode.Clear, which needs its own layer — without
            // it the cut would punch through whatever is painted behind the icon.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val radius = size.minDimension * 0.42f
        drawCircle(color = tint, radius = radius, center = center)
        drawCircle(
            color = Color.Black,
            radius = radius * 0.92f,
            center = center + Offset(radius * 0.62f, -radius * 0.42f),
            blendMode = BlendMode.Clear,
        )
    }
}
