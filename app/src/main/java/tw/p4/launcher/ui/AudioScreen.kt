package tw.p4.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tw.p4.launcher.LauncherViewModel

/**
 * 音訊：播放控制鍵與「選擇來源」。
 * 不讀取播放中的歌名（那需要「通知存取」，在這台車機上疑似影響方向盤按鍵）；
 * 控制鍵送出系統媒體按鍵，由目前播放中的 App 處理。
 */
@Composable
fun AudioScreen(vm: LauncherViewModel) {
    val c = LocalCamry.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(Icons.Rounded.MusicNote, "音訊")
        Box(Modifier.weight(1f).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = MicClearance)) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.MusicNote, null, Modifier.size(72.dp), tint = c.text)
                Spacer(Modifier.height(16.dp))
                Text("控制目前播放中的音樂，或選擇音訊來源。", color = c.text, fontSize = 24.sp)
                Spacer(Modifier.height(32.dp))
                TransportControls(vm, big = 84.dp)
                Spacer(Modifier.height(32.dp))
                PillButton("選擇來源", Modifier.width(280.dp)) { vm.showSources = true }
            }
        }
    }
}
