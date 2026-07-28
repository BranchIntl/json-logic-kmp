package co.branch.jsonlogic.playground.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import co.branch.jsonlogic.playground.resources.Res
import co.branch.jsonlogic.playground.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

@Immutable
data class SyntaxColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val literal: Color,
    val punctuation: Color,
    val invalid: Color,
)

/** The parts of the palette Material 3's colour scheme has no slot for. */
@Immutable
data class PlaygroundColors(
    val panel: Color,
    val panelBorder: Color,
    val gutter: Color,
    val chip: Color,
    val ok: Color,
    val syntax: SyntaxColors,
)

val LocalPlaygroundColors = staticCompositionLocalOf<PlaygroundColors> {
    error("No PlaygroundColors: wrap the content in PlaygroundTheme.")
}

val LocalMonospaceStyle = staticCompositionLocalOf<TextStyle> {
    error("No monospace style: wrap the content in PlaygroundTheme.")
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5B5BD6),
    onPrimary = Color.White,
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF16181D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFEFEFF4),
    onSurfaceVariant = Color(0xFF6B6F7B),
    error = Color(0xFFC7283A),
    outline = Color(0xFFE3E4EA),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9B9BF5),
    onPrimary = Color(0xFF1B1B33),
    background = Color(0xFF131519),
    onBackground = Color(0xFFE8EAEF),
    surface = Color(0xFF1B1E24),
    onSurface = Color(0xFFE8EAEF),
    surfaceVariant = Color(0xFF23272E),
    onSurfaceVariant = Color(0xFF9AA0AC),
    error = Color(0xFFFF8A93),
    outline = Color(0xFF2B2F37),
)

private val LightPlayground = PlaygroundColors(
    panel = Color(0xFFFFFFFF),
    panelBorder = Color(0xFFE3E4EA),
    gutter = Color(0xFFAFB3BE),
    chip = Color(0xFFEFEFF4),
    ok = Color(0xFF14895D),
    syntax = SyntaxColors(
        key = Color(0xFF0550AE),
        string = Color(0xFF0A7B4B),
        number = Color(0xFF953800),
        literal = Color(0xFF8250DF),
        punctuation = Color(0xFF57606A),
        invalid = Color(0xFFCF222E),
    ),
)

private val DarkPlayground = PlaygroundColors(
    panel = Color(0xFF1B1E24),
    panelBorder = Color(0xFF2B2F37),
    gutter = Color(0xFF565C68),
    chip = Color(0xFF23272E),
    ok = Color(0xFF3FC98E),
    syntax = SyntaxColors(
        key = Color(0xFF79C0FF),
        string = Color(0xFF7EE787),
        number = Color(0xFFFFA657),
        literal = Color(0xFFD2A8FF),
        punctuation = Color(0xFF8B949E),
        invalid = Color(0xFFFF7B72),
    ),
)

private val PlaygroundTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        bodyMedium = bodyMedium.copy(fontSize = 13.sp),
        bodySmall = bodySmall.copy(fontSize = 12.sp),
        labelMedium = labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.7.sp),
    )
}

@Composable
fun PlaygroundTheme(dark: Boolean, content: @Composable () -> Unit) {
    // Bundled, not named: FontFamily.Monospace falls back to the proportional default under the
    // web renderer, which misaligns the JSON and the gutter measured against it.
    val monospace = TextStyle(
        fontFamily = FontFamily(Font(Res.font.jetbrains_mono_regular)),
        fontSize = 13.sp,
        lineHeight = 21.sp,
        // JetBrains Mono draws >= as ≥ and != as ≠. These are operator names the reader has to
        // be able to type back.
        fontFeatureSettings = "liga 0, calt 0",
    )

    CompositionLocalProvider(
        LocalPlaygroundColors provides if (dark) DarkPlayground else LightPlayground,
        LocalMonospaceStyle provides monospace,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = PlaygroundTypography,
            content = content,
        )
    }
}
