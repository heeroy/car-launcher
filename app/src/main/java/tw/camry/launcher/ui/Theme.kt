package tw.camry.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CamryColors(
    val bg: Color,
    val rail: Color,
    val surface: Color,
    val surfaceHi: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val onAccent: Color,
    val divider: Color,
)

val NightColors = CamryColors(
    bg = Color(0xFF0E1114),
    rail = Color(0xFF14181C),
    surface = Color(0xFF1B2026),
    surfaceHi = Color(0xFF262D35),
    text = Color(0xFFF2F4F6),
    textDim = Color(0xFF8F99A4),
    accent = Color(0xFFEB0A1E),
    onAccent = Color.White,
    divider = Color(0xFF2B323A),
)

val DayColors = CamryColors(
    bg = Color(0xFFE9ECEF),
    rail = Color(0xFFF7F8F9),
    surface = Color.White,
    surfaceHi = Color(0xFFE2E6EA),
    text = Color(0xFF14181C),
    textDim = Color(0xFF626C77),
    accent = Color(0xFFEB0A1E),
    onAccent = Color.White,
    divider = Color(0xFFD3D8DD),
)

val LocalCamry = staticCompositionLocalOf { NightColors }

@Composable
fun CamryTheme(night: Boolean, content: @Composable () -> Unit) {
    val c = if (night) NightColors else DayColors
    val scheme = if (night) {
        darkColorScheme(primary = c.accent, onPrimary = c.onAccent, background = c.bg, surface = c.surface, onSurface = c.text)
    } else {
        lightColorScheme(primary = c.accent, onPrimary = c.onAccent, background = c.bg, surface = c.surface, onSurface = c.text)
    }
    CompositionLocalProvider(LocalCamry provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
