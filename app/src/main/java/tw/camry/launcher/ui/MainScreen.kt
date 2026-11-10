package tw.camry.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tw.camry.launcher.LauncherViewModel
import tw.camry.launcher.Screen
import tw.camry.launcher.data.Slots

@Composable
fun CamryApp(vm: LauncherViewModel) {
    CamryTheme(night = vm.isNight) {
        val c = LocalCamry.current
        BackHandler(enabled = vm.screen != Screen.HOME) { vm.go(Screen.HOME) }
        Row(Modifier.fillMaxSize().background(c.bg)) {
            MenuRail(vm)
            Column(Modifier.weight(1f).fillMaxHeight()) {
                TopBar(vm)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (vm.screen) {
                        Screen.HOME -> HomeScreen(vm)
                        Screen.AUDIO -> AudioScreen(vm)
                        Screen.APPS -> AppsScreen(vm)
                        Screen.SETTINGS -> SettingsScreen(vm)
                    }
                }
            }
        }
    }
}

/** 主選單：依手冊順序（CarPlay、Android Auto、導航、音訊、電話、車輛資訊、設定），另加主頁與應用程式。 */
@Composable
private fun MenuRail(vm: LauncherViewModel) {
    val c = LocalCamry.current
    Column(
        Modifier.width(112.dp).fillMaxHeight().background(c.rail).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 麥克風按鈕
        Box(Modifier.weight(0.9f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(c.surfaceHi).clickable { vm.startVoice() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Mic, "語音", Modifier.size(30.dp), tint = c.text) }
        }
        RailItem("主頁", Icons.Rounded.Home, vm.screen == Screen.HOME) { vm.go(Screen.HOME) }
        if (vm.bindings.containsKey(Slots.CARPLAY))
            RailItem("CarPlay", Icons.Rounded.PhoneIphone, false) { vm.open(Slots.CARPLAY) }
        if (vm.bindings.containsKey(Slots.ANDROID_AUTO))
            RailItem("Android Auto", Icons.Rounded.Android, false) { vm.open(Slots.ANDROID_AUTO) }
        RailItem("導航", Icons.Rounded.Navigation, false) { vm.open(Slots.NAV) }
        RailItem("音訊", Icons.Rounded.MusicNote, vm.screen == Screen.AUDIO) { vm.go(Screen.AUDIO) }
        RailItem("電話", Icons.Rounded.Call, false) { vm.open(Slots.PHONE) }
        RailItem("車輛資訊", Icons.Rounded.DirectionsCar, false) { vm.open(Slots.VEHICLE) }
        RailItem("應用程式", Icons.Rounded.Apps, vm.screen == Screen.APPS) { vm.go(Screen.APPS) }
        RailItem("設定", Icons.Rounded.Settings, vm.screen == Screen.SETTINGS) { vm.go(Screen.SETTINGS) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.RailItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalCamry.current
    Box(
        Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) c.accent else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, label, Modifier.size(32.dp), tint = if (selected) c.onAccent else c.textDim)
            Text(
                label,
                color = if (selected) c.onAccent else c.textDim,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

/** 畫面頂端：左側標題／返回，右側為通訊狀態圖示與時間（手冊「狀態圖示」）。 */
@Composable
private fun TopBar(vm: LauncherViewModel) {
    val c = LocalCamry.current
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (vm.screen != Screen.HOME) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable { vm.go(Screen.HOME) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.ChevronLeft, "返回", Modifier.size(32.dp), tint = c.text) }
            Spacer(Modifier.width(8.dp))
        }
        Text(vm.screen.title, color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

        if (vm.bluetoothOn) StatusIcon(Icons.Rounded.Bluetooth, "藍牙")
        if (vm.cellular) StatusIcon(Icons.Rounded.SignalCellularAlt, "行動網路")
        if (vm.wifi) StatusIcon(Icons.Rounded.Wifi, "Wi-Fi")
        Spacer(Modifier.width(14.dp))
        Text("%02d:%02d".format(vm.now.hour, vm.now.minute), color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusIcon(icon: ImageVector, desc: String) {
    val c = LocalCamry.current
    Icon(icon, desc, Modifier.padding(start = 12.dp).size(26.dp), tint = c.textDim)
}
