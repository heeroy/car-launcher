package tw.p4.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 顏色依實車照片取近似值（照片有曝光誤差，不是原廠色碼）。 */
data class CamryColors(
    val bg: Color,
    val rail: Color,
    /** 左側子選單、卡片 */
    val surface: Color,
    val surfaceHi: Color,
    val text: Color,
    val textDim: Color,
    /** 選單選中的圓底、實心按鈕 */
    val accent: Color,
    val onAccent: Color,
    /** 外框膠囊按鈕的框線 */
    val outline: Color,
    val divider: Color,
    /** 浮動麥克風按鈕（淺藍漸層） */
    val micTop: Color,
    val micBottom: Color,
    val onMic: Color,
    /** 音訊來源全螢幕畫面（藍紫漸層） */
    val sourceTop: Color,
    val sourceBottom: Color,
)

val NightColors = CamryColors(
    bg = Color(0xFF2B3245),
    rail = Color(0xFF242A3B),
    surface = Color(0xFF343C52),
    surfaceHi = Color(0xFF465070),
    text = Color(0xFFF2F4F8),
    textDim = Color(0xFFA3ABBD),
    accent = Color(0xFF3D66D6),
    onAccent = Color.White,
    outline = Color(0xFF8FB0F0),
    divider = Color(0xFF55607E),
    micTop = Color(0xFFE6EEFF),
    micBottom = Color(0xFF8FB3F5),
    onMic = Color(0xFF2A4FB5),
    sourceTop = Color(0xFF4A7BE8),
    sourceBottom = Color(0xFF8A6FE0),
)

val DayColors = CamryColors(
    bg = Color(0xFFE6EAF2),
    rail = Color(0xFFFFFFFF),
    surface = Color(0xFFF4F6FA),
    surfaceHi = Color(0xFFD9E0EE),
    text = Color(0xFF1B2233),
    textDim = Color(0xFF5E6880),
    accent = Color(0xFF2F5FD0),
    onAccent = Color.White,
    outline = Color(0xFF2F5FD0),
    divider = Color(0xFFC9D1E0),
    micTop = Color(0xFFE6EEFF),
    micBottom = Color(0xFF8FB3F5),
    onMic = Color(0xFF2A4FB5),
    sourceTop = Color(0xFF5C8BF0),
    sourceBottom = Color(0xFF9A80EA),
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
