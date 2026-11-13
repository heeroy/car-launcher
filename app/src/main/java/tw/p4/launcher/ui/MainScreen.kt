package tw.p4.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.p4.launcher.LauncherViewModel
import tw.p4.launcher.Screen
import tw.p4.launcher.SettingsSection
import tw.p4.launcher.data.Slots
import tw.p4.launcher.data.formatClock

@Composable
fun CamryApp(vm: LauncherViewModel) {
    CamryTheme(night = vm.isNight) {
        val c = LocalCamry.current
        BackHandler(enabled = vm.showSources || vm.screen != Screen.HOME) {
            if (vm.showSources) vm.showSources = false else vm.go(Screen.HOME)
        }
        Box(Modifier.fillMaxSize().background(c.bg)) {
            Row(Modifier.fillMaxSize()) {
                MenuRail(vm)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (vm.screen) {
                        Screen.HOME -> HomeScreen(vm)
                        Screen.AUDIO -> AudioScreen(vm)
                        Screen.APPS -> AppsScreen(vm)
                        Screen.SETTINGS -> SettingsScreen(vm)
                    }
                    StatusCluster(vm, Modifier.align(Alignment.TopEnd))
                    MicCluster(vm, Modifier.align(Alignment.BottomStart))
                }
            }
            if (vm.showSources) SourceOverlay(vm)
        }
    }
}

/**
 * 左側主選單：只有圖示，選中時為藍色圓底。
 * CarPlay／Android Auto 只在「有綁定且偵測到連線」時出現在最上面（與實車相同）；
 * 主頁與應用程式是實車沒有、但本桌面需要的入口。
 */
@Composable
private fun MenuRail(vm: LauncherViewModel) {
    val c = LocalCamry.current
    Column(
        Modifier.width(96.dp).fillMaxHeight().background(c.rail).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (vm.showCarPlay) RailItem("CarPlay", Icons.Rounded.PhoneIphone, false) { vm.open(Slots.CARPLAY) }
        if (vm.showAndroidAuto) RailItem("Android Auto", Icons.Rounded.Android, false) { vm.open(Slots.ANDROID_AUTO) }
        RailItem("主頁", Icons.Rounded.Home, vm.screen == Screen.HOME) { vm.go(Screen.HOME) }
        RailItem("導航", Icons.Rounded.Navigation, false) { vm.open(Slots.NAV) }
        RailItem("音訊", Icons.Rounded.MusicNote, vm.screen == Screen.AUDIO) { vm.go(Screen.AUDIO) }
        RailItem("電話", Icons.Rounded.Call, false) { vm.open(Slots.PHONE) }
        RailItem("車輛資訊", Icons.Rounded.DirectionsCar, false) { vm.open(Slots.VEHICLE) }
        RailItem("應用程式", Icons.Rounded.Apps, vm.screen == Screen.APPS) { vm.go(Screen.APPS) }
        RailItem("設定", Icons.Rounded.Settings, vm.screen == Screen.SETTINGS) { vm.go(Screen.SETTINGS) }
    }
}

@Composable
private fun ColumnScope.RailItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val c = LocalCamry.current
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(if (selected) c.accent else Color.Transparent)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, Modifier.size(34.dp), tint = if (selected) c.onAccent else c.text)
        }
    }
}

/** 右上角：通訊狀態圖示與時間（實車為 12 小時制「2:43 PM」）。 */
@Composable
private fun StatusCluster(vm: LauncherViewModel, modifier: Modifier) {
    val c = LocalCamry.current
    Row(modifier.height(StatusBarHeight).padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        if (vm.bluetoothOn) StatusIcon(Icons.Rounded.Bluetooth, "藍牙")
        if (vm.cellular) StatusIcon(Icons.Rounded.SignalCellularAlt, "行動網路")
        if (vm.wifi) StatusIcon(Icons.Rounded.Wifi, "Wi-Fi")
        Spacer(Modifier.width(16.dp))
        Text(formatClock(vm.now.hour, vm.now.minute, vm.clock24h), color = c.text, fontSize = 30.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusIcon(icon: ImageVector, desc: String) {
    val c = LocalCamry.current
    Icon(icon, desc, Modifier.padding(start = 12.dp).size(26.dp), tint = c.text)
}

/** 左下角浮動的麥克風；音訊畫面旁邊加「來源」。 */
@Composable
private fun MicCluster(vm: LauncherViewModel, modifier: Modifier) {
    val c = LocalCamry.current
    Row(modifier.padding(start = 24.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        MicButton { vm.startVoice() }
        if (vm.screen == Screen.AUDIO) {
            Spacer(Modifier.width(16.dp))
            Box(
                Modifier.height(MicSize).width(220.dp).clip(RoundedCornerShape(50))
                    .background(c.surfaceHi.copy(alpha = 0.92f))
                    .clickable { vm.showSources = true },
                contentAlignment = Alignment.Center,
            ) { Text("來源", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

/** 音訊「來源」全螢幕畫面：藍紫漸層、左上角 ✕、右上角「管理裝置」（實車照片的樣式）。 */
@Composable
private fun SourceOverlay(vm: LauncherViewModel) {
    val c = LocalCamry.current
    val np by vm.media.state.collectAsStateWithLifecycle()
    val sources = Slots.MUSIC.mapNotNull { vm.appFor(it) }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(c.sourceTop, c.sourceBottom)))
            // 吃掉點擊，避免點到下層（不顯示點擊效果）
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        Box(
            Modifier.align(Alignment.TopStart).padding(16.dp).size(64.dp).clip(CircleShape)
                .clickable { vm.showSources = false },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Close, "關閉", Modifier.size(38.dp), tint = Color.White) }

        Row(
            Modifier.align(Alignment.TopEnd).padding(24.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xE61B2233))
                .clickable { vm.openSettings(SettingsSection.BINDINGS) }
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Settings, null, Modifier.size(24.dp), tint = Color(0xFF8FB0F0))
            Spacer(Modifier.width(10.dp))
            Text("管理裝置", color = Color(0xFF8FB0F0), fontSize = 20.sp)
        }

        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(0.5f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (sources.isEmpty()) {
                Text("尚未綁定音訊來源", color = Color.White, fontSize = 32.sp)
                Text("點右上角「管理裝置」，在 App 綁定裡指定音訊來源", color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp)
            }
            sources.forEach { app ->
                val active = np.pkg == app.pkg
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable { vm.showSources = false; vm.launchPackage(app.pkg) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(app.icon, null, Modifier.size(52.dp))
                    Spacer(Modifier.width(22.dp))
                    Text(app.label, color = Color.White, fontSize = 40.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
