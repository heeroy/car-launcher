package tw.camry.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import tw.camry.launcher.data.Slots
import tw.camry.launcher.data.formatDuration

/** 音訊：左側子選單為「來源」，右側主要區域為播放畫面（手冊「基本畫面功能」的子選單＋主要區域結構）。 */
@Composable
fun AudioScreen(vm: LauncherViewModel) {
    val c = LocalCamry.current
    val np by vm.media.state.collectAsStateWithLifecycle()
    val sources = Slots.MUSIC.mapNotNull { vm.appFor(it) }

    Row(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        // 子選單：來源
        Panel(Modifier.width(300.dp).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("來源", color = c.textDim, fontSize = 16.sp, modifier = Modifier.padding(8.dp))
                if (sources.isEmpty()) {
                    Text("尚未綁定音訊來源\n設定 → App 綁定", color = c.textDim, fontSize = 17.sp, modifier = Modifier.padding(8.dp))
                }
                sources.forEach { app ->
                    val active = np.pkg == app.pkg
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) c.surfaceHi else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { vm.launchPackage(app.pkg) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(app.icon, app.label, Modifier.size(44.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, color = c.text, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // 主要區域
        Panel(Modifier.weight(1f).fillMaxHeight()) {
            if (!np.hasAccess) {
                Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("需要「通知存取」才能顯示播放中的音樂", color = c.text, fontSize = 24.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("只會讀取目前播放的歌名、封面與進度，不讀取通知內容", color = c.textDim, fontSize = 17.sp)
                    Spacer(Modifier.height(24.dp))
                    PillButton("前往開啟") { vm.openNotificationAccess() }
                }
            } else {
                NowPlayingBody(np, vm)
            }
        }
    }
}

@Composable
private fun NowPlayingBody(np: tw.camry.launcher.media.NowPlaying, vm: LauncherViewModel) {
    val c = LocalCamry.current
    Row(Modifier.fillMaxSize().padding(36.dp), verticalAlignment = Alignment.CenterVertically) {
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
