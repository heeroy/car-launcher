package tw.camry.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.os.SystemClock
import tw.camry.launcher.LauncherViewModel
import tw.camry.launcher.data.AppEntry
import tw.camry.launcher.media.NowPlaying
import tw.camry.launcher.media.livePosition

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalCamry.current
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

/** 膠囊按鈕：預設為實車常見的外框樣式；filled 用於「目前選中」的選項。 */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, filled: Boolean = false, onClick: () -> Unit) {
    val c = LocalCamry.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .then(if (filled) Modifier.background(c.accent) else Modifier.border(1.5.dp, c.outline, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (filled) c.onAccent else c.text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

/** 畫面左上角的標題（圖示＋文字），高度與右上角的狀態列一致。 */
@Composable
fun ScreenHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    val c = LocalCamry.current
    Row(modifier.height(StatusBarHeight).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(28.dp), tint = c.text)
        Spacer(Modifier.width(14.dp))
        Text(title, color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 分組標題：小字＋底線（例如實車設定的「目前駕駛」「我的設定」）。 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val c = LocalCamry.current
    Column(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
        Text(text, color = c.textDim, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    }
}

/** 子選單項目：大字，右側 >；選中時加底色。 */
@Composable
fun SubMenuRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalCamry.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.surfaceHi else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(28.dp), tint = c.textDim)
    }
}

/** 浮動麥克風按鈕（內容區左下角）。 */
@Composable
fun MicButton(onClick: () -> Unit) {
    val c = LocalCamry.current
    Box(
        Modifier.size(MicSize).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(c.micTop, c.micBottom)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Mic, "語音", Modifier.size(32.dp), tint = c.onMic) }
}

val StatusBarHeight: Dp = 64.dp
val MicSize: Dp = 68.dp
/** 讓內容避開左下角浮動麥克風的底部留白 */
val MicClearance: Dp = 108.dp

@Composable
fun CircleIconButton(
    icon: ImageVector,
    desc: String,
    size: Dp,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = LocalCamry.current
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) c.accent else c.surfaceHi)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, desc, Modifier.size(size * 0.55f),
            tint = if (!enabled) c.textDim.copy(alpha = 0.4f) else if (filled) c.onAccent else c.text,
        )
    }
}

@Composable
fun TransportControls(np: NowPlaying, vm: LauncherViewModel, big: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleIconButton(Icons.Rounded.SkipPrevious, "上一首", big * 0.75f, enabled = np.hasSession) { vm.media.previous() }
        CircleIconButton(
            if (np.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (np.playing) "暫停" else "播放", big, filled = true, enabled = np.hasSession,
        ) { vm.media.playPause() }
        CircleIconButton(Icons.Rounded.SkipNext, "下一首", big * 0.75f, enabled = np.hasSession) { vm.media.next() }
    }
}

/** 每 500ms 依 PlaybackState 推算目前進度。 */
@Composable
fun rememberLivePosition(np: NowPlaying): Long {
    val pos by produceState(np.position, np) {
        while (true) {
            value = livePosition(np, SystemClock.elapsedRealtime())
            kotlinx.coroutines.delay(500)
        }
    }
    return pos
}

@Composable
fun AppIconTile(
    app: AppEntry,
    modifier: Modifier = Modifier,
    iconSize: Dp = 64.dp,
    onClick: () -> Unit,
) {
    val c = LocalCamry.current
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(app.icon, app.label, Modifier.size(iconSize), contentScale = ContentScale.Fit)
        Spacer(Modifier.size(8.dp))
        Text(app.label, color = c.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppEntry>,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalCamry.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.84f)) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    PillButton("清除綁定", filled = false) { onPick(null) }
                    Spacer(Modifier.width(12.dp))
                    PillButton("取消", filled = false, onClick = onDismiss)
                }
                Spacer(Modifier.size(12.dp))
                LazyVerticalGrid(GridCells.Adaptive(130.dp), Modifier.weight(1f)) {
                    items(apps, key = { it.pkg }) { app -> AppIconTile(app) { onPick(app.pkg) } }
                }
            }
        }
    }
}
