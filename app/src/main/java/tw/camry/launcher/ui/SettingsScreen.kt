package tw.camry.launcher.ui

import android.os.Build
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.camry.launcher.BuildConfig
import tw.camry.launcher.LauncherViewModel
import tw.camry.launcher.Screen
import tw.camry.launcher.SettingsSection
import tw.camry.launcher.data.ProjectionMode
import tw.camry.launcher.data.Slots
import tw.camry.launcher.data.ThemeMode

/**
 * 設定：左側子選單（「⚙ 設定」標題＋大字項目＋ >），右側主要區域，中間一條分隔線（實車照片的版型）。
 * 左下角有浮動麥克風，所以子選單底部留白。
 */
@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val c = LocalCamry.current
    val section = vm.settingsSection
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(0.4f).fillMaxHeight().background(c.surface)) {
            ScreenHeader(Icons.Rounded.Settings, Screen.SETTINGS.title)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = MicClearance),
            ) {
                SettingsSection.values().forEach { s ->
                    SubMenuRow(s.title, selected = s == section) { vm.settingsSection = s }
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.divider))
        Column(
            Modifier.weight(0.6f).fillMaxHeight().verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = StatusBarHeight, bottom = 24.dp),
        ) {
            when (section) {
                SettingsSection.DISPLAY -> DisplaySection(vm)
                SettingsSection.BINDINGS -> BindingsSection(vm)
                SettingsSection.SYSTEM -> SystemSection(vm)
                SettingsSection.ABOUT -> AboutSection(vm)
            }
        }
    }
}

@Composable
private fun DisplaySection(vm: LauncherViewModel) {
    val c = LocalCamry.current
    SectionLabel("日夜模式")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(ThemeMode.AUTO to "自動（依時間）", ThemeMode.DAY to "日間", ThemeMode.NIGHT to "夜間").forEach { (m, label) ->
            PillButton(label, filled = vm.themeMode == m) { vm.updateThemeMode(m) }
        }
    }
    Spacer(Modifier.height(28.dp))
    HourStepper("日間開始", vm.dayStart, enabled = vm.themeMode == ThemeMode.AUTO) { vm.updateDayStart(it) }
    Spacer(Modifier.height(12.dp))
    HourStepper("夜間開始", vm.nightStart, enabled = vm.themeMode == ThemeMode.AUTO) { vm.updateNightStart(it) }
    Spacer(Modifier.height(20.dp))
    Text(
        "目前為「${if (vm.isNight) "夜間" else "日間"}」配色。此設定只改變桌面配色，不會更動車機螢幕亮度。",
        color = c.textDim, fontSize = 16.sp,
    )

    Spacer(Modifier.height(20.dp))
    SectionLabel("時間格式")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton("12 小時制（2:43 PM）", filled = !vm.clock24h) { vm.updateClock24h(false) }
        PillButton("24 小時制（14:43）", filled = vm.clock24h) { vm.updateClock24h(true) }
    }

    Spacer(Modifier.height(20.dp))
    SectionLabel("左側選單的 CarPlay／Android Auto")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton("偵測到連線才顯示", filled = vm.projectionMode == ProjectionMode.AUTO) { vm.updateProjectionMode(ProjectionMode.AUTO) }
        PillButton("有綁定就一律顯示", filled = vm.projectionMode == ProjectionMode.ALWAYS) { vm.updateProjectionMode(ProjectionMode.ALWAYS) }
    }
    Spacer(Modifier.height(8.dp))
    Text("都需要先在「App 綁定」指定 App。偵測不準時可改成一律顯示；偵測狀態在「權限與系統」。", color = c.textDim, fontSize = 16.sp)
}

@Composable
private fun HourStepper(label: String, hour: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val c = LocalCamry.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (enabled) c.text else c.textDim, fontSize = 20.sp, modifier = Modifier.width(160.dp))
        CircleIconButton(Icons.Rounded.Remove, "減少", 48.dp, enabled = enabled) { onChange((hour + 23) % 24) }
        Text("%02d:00".format(hour), color = c.text, fontSize = 24.sp, modifier = Modifier.width(110.dp).padding(horizontal = 12.dp))
        CircleIconButton(Icons.Rounded.Add, "增加", 48.dp, enabled = enabled) { onChange((hour + 1) % 24) }
    }
}

@Composable
private fun BindingsSection(vm: LauncherViewModel) {
    val c = LocalCamry.current
    var pickerFor by remember { mutableStateOf<String?>(null) }
    SectionLabel("App 綁定")
    Text("選擇每個入口要開啟哪個 App。第一次開啟時已依名稱自動猜測，猜錯可在這裡修改。", color = c.textDim, fontSize = 16.sp)
    Spacer(Modifier.height(14.dp))
    Slots.all.forEach { key ->
        val app = vm.appFor(key)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 3.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.surfaceHi.copy(alpha = 0.5f))
                .clickable { pickerFor = key }
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(Slots.labels[key] ?: key, color = c.text, fontSize = 20.sp, modifier = Modifier.weight(1f))
            if (app != null) {
                Image(app.icon, null, Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
                Text(app.label, color = c.textDim, fontSize = 18.sp)
            } else {
                Text("未綁定", color = c.textDim, fontSize = 18.sp)
            }
        }
    }
    pickerFor?.let { key ->
        AppPickerDialog(
            title = Slots.labels[key] ?: "選擇 App",
            apps = vm.apps,
            onPick = { vm.bind(key, it); pickerFor = null },
            onDismiss = { pickerFor = null },
        )
    }
}

@Composable
private fun SystemSection(vm: LauncherViewModel) {
    val c = LocalCamry.current
    val np by vm.media.state.collectAsStateWithLifecycle()
    SectionLabel("通知存取")
    Text("通知存取：${if (np.hasAccess) "已允許" else "未允許（音訊畫面無法顯示播放中的音樂）"}", color = c.text, fontSize = 20.sp)
    Spacer(Modifier.height(12.dp))
    Text("用來讀取播放中的音樂，以及偵測 CarPlay／Android Auto 的常駐通知。", color = c.textDim, fontSize = 16.sp)
    Spacer(Modifier.height(12.dp))
    PillButton("開啟通知存取設定") { vm.openNotificationAccess() }
    Spacer(Modifier.height(24.dp))
    ProjectionDiagnostics(vm)
    Spacer(Modifier.height(24.dp))
    SectionLabel("預設桌面")
    Text("要換回原廠桌面，或把本桌面設為預設首頁，請用下方按鈕。", color = c.textDim, fontSize = 16.sp)
    Spacer(Modifier.height(12.dp))
    PillButton("變更預設桌面") { vm.openHomeChooser() }
    Spacer(Modifier.height(16.dp))
    PillButton("系統設定") { vm.openSystemSettings() }
}

/** CarPlay／Android Auto 偵測狀態：在車上確認偵測準不準用。 */
@Composable
private fun ProjectionDiagnostics(vm: LauncherViewModel) {
    val c = LocalCamry.current
    SectionLabel("CarPlay／Android Auto 偵測狀態")
    Text("連上與拔掉手機時看這裡的變化（每 5 秒更新）。如果偵測不準，請把這裡的內容拍照給我。", color = c.textDim, fontSize = 16.sp)
    Spacer(Modifier.height(8.dp))
    listOf(
        Triple("CarPlay", Slots.CARPLAY, vm.carPlayStatus to vm.showCarPlay),
        Triple("Android Auto", Slots.ANDROID_AUTO, vm.androidAutoStatus to vm.showAndroidAuto),
    ).forEach { (name, slot, pair) ->
        val (status, shown) = pair
        val pkg = vm.bindings[slot]
        val app = vm.appFor(slot)
        Column(
            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp)).background(c.surfaceHi.copy(alpha = 0.5f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text("$name：${if (shown) "左側選單顯示中" else "不顯示"}", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            DiagLine("綁定", app?.let { "${it.label}（${it.pkg}）" } ?: pkg ?: "未綁定")
            DiagLine("USB", if (status.usb) "偵測到" else "沒有")
            DiagLine("常駐通知", if (status.notification) "有" else "沒有")
            pkg?.let { vm.projectionTitles[it] }?.forEach { DiagLine("　通知內容", it) }
        }
    }
    Spacer(Modifier.height(4.dp))
    DiagLine(
        "USB 裝置",
        vm.usbDevices.joinToString("、") { "%04X:%04X %s".format(it.vendorId, it.productId, it.name) }.ifBlank { "（無）" },
    )
}

@Composable
private fun DiagLine(k: String, v: String) {
    val c = LocalCamry.current
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(k, color = c.textDim, fontSize = 17.sp, modifier = Modifier.width(120.dp))
        Text(v, color = c.text, fontSize = 17.sp)
    }
}

@Composable
private fun AboutSection(vm: LauncherViewModel) {
    val c = LocalCamry.current
    val cfg = LocalConfiguration.current
    val lines = listOf(
        "版本" to BuildConfig.VERSION_NAME,
        "裝置" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）",
        "螢幕" to "${cfg.screenWidthDp}×${cfg.screenHeightDp} dp（${cfg.densityDpi} dpi）",
        "已偵測 App" to "${vm.apps.size}",
    )
    SectionLabel("關於")
    lines.forEach { (k, v) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(k, color = c.textDim, fontSize = 19.sp, modifier = Modifier.width(180.dp))
            Text(v, color = c.text, fontSize = 19.sp)
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("介面版型參考 Toyota Camry 多媒體系統的主選單、狀態圖示與子選單／主要區域結構；本專案與 Toyota 無關。", color = c.textDim, fontSize = 15.sp)
}
