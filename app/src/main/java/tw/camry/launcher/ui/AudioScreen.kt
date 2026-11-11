package tw.camry.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.camry.launcher.LauncherViewModel
import tw.camry.launcher.data.formatDuration

/**
 * 音訊：整個寬度的播放畫面，左上角標題是目前的來源（實車沒有播放時顯示「音訊關閉」）。
 * 要切換來源，按左下角麥克風旁的「來源」。
 */
@Composable
fun AudioScreen(vm: LauncherViewModel) {
    val c = LocalCamry.current
    val np by vm.media.state.collectAsStateWithLifecycle()
    val sourceLabel = np.pkg?.let { pkg -> vm.apps.firstOrNull { it.pkg == pkg }?.label }
    val title = when {
        np.hasSession -> sourceLabel ?: "音訊"
        else -> "音訊關閉"
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(Icons.Rounded.MusicNote, title)
        Box(Modifier.weight(1f).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = MicClearance)) {
            when {
                !np.hasAccess -> CenterMessage(
                    "需要「通知存取」才能顯示播放中的音樂",
                    "只會讀取目前播放的歌名、封面與進度",
                    "前往開啟",
                ) { vm.openNotificationAccess() }
                !np.hasSession -> CenterMessage(
                    "若要播放音樂，請選擇音訊來源。",
                    null,
                    "選擇來源",
                ) { vm.showSources = true }
                else -> NowPlayingBody(np, vm)
            }
        }
    }
}

/** 實車空白狀態的樣式：音符圖示、一行說明、一個外框按鈕。 */
@Composable
private fun BoxScope.CenterMessage(text: String, sub: String?, action: String, onAction: () -> Unit) {
    val c = LocalCamry.current
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.MusicNote, null, Modifier.size(72.dp), tint = c.text)
        Spacer(Modifier.height(16.dp))
        Text(text, color = c.text, fontSize = 24.sp)
        if (sub != null) {
            Spacer(Modifier.height(6.dp))
            Text(sub, color = c.textDim, fontSize = 17.sp)
        }
        Spacer(Modifier.height(28.dp))
        PillButton(action, Modifier.width(280.dp), onClick = onAction)
    }
}

@Composable
private fun NowPlayingBody(np: tw.camry.launcher.media.NowPlaying, vm: LauncherViewModel) {
    val c = LocalCamry.current
    Row(Modifier.fillMaxSize().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.fillMaxHeight(0.82f).aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)).background(c.surfaceHi),
            contentAlignment = Alignment.Center,
        ) {
            val art = np.art
            if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Rounded.MusicNote, null, Modifier.size(96.dp), tint = c.textDim)
        }
        Spacer(Modifier.width(40.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                if (np.hasSession && np.title.isNotBlank()) np.title else "尚無播放中的音樂",
                color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (np.hasSession) {
                Spacer(Modifier.height(6.dp))
                Text(np.artist.ifBlank { "—" }, color = c.textDim, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (np.album.isNotBlank())
                    Text(np.album, color = c.textDim, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(28.dp))

            val live = rememberLivePosition(np)
            var dragging by remember { mutableStateOf<Float?>(null) }
            val dur = np.duration.coerceAtLeast(0)
            val shown = dragging ?: if (dur > 0) live.toFloat() else 0f
            Slider(
                value = shown.coerceIn(0f, if (dur > 0) dur.toFloat() else 1f),
                onValueChange = { dragging = it },
                onValueChangeFinished = { dragging?.let { vm.media.seekTo(it.toLong()) }; dragging = null },
                valueRange = 0f..(if (dur > 0) dur.toFloat() else 1f),
                enabled = np.hasSession && np.canSeek && dur > 0,
                colors = SliderDefaults.colors(
                    thumbColor = c.accent, activeTrackColor = c.accent,
                    inactiveTrackColor = c.surfaceHi,
                    disabledThumbColor = c.textDim, disabledActiveTrackColor = c.textDim.copy(alpha = 0.6f),
                    disabledInactiveTrackColor = c.surfaceHi,
                ),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatDuration(shown.toLong()), color = c.textDim, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(formatDuration(dur), color = c.textDim, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
            TransportControls(np, vm, big = 84.dp)
        }
    }
}
