package tw.camry.launcher

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tw.camry.launcher.data.AppEntry
import tw.camry.launcher.data.AppRepository
import tw.camry.launcher.data.Defaults
import tw.camry.launcher.data.Prefs
import tw.camry.launcher.data.ProjectionMode
import tw.camry.launcher.data.ProjectionSignals
import tw.camry.launcher.data.ProjectionStatus
import tw.camry.launcher.data.UsbInfo
import tw.camry.launcher.data.isAndroidAccessoryUsb
import tw.camry.launcher.data.isAppleUsb
import tw.camry.launcher.data.projectionStatus
import tw.camry.launcher.data.showProjection
import tw.camry.launcher.data.Slots
import tw.camry.launcher.data.ThemeMode
import tw.camry.launcher.data.resolveNight
import tw.camry.launcher.media.MediaMonitor
import java.time.LocalDateTime

enum class Screen(val title: String) {
    HOME("主頁"), AUDIO("音訊"), APPS("應用程式"), SETTINGS("設定")
}

enum class SettingsSection(val title: String) {
    DISPLAY("顯示"), BINDINGS("App 綁定"), SYSTEM("權限與系統"), ABOUT("關於")
}

class LauncherViewModel(private val app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val repo = AppRepository(app)
    val media = MediaMonitor(app)

    var screen by mutableStateOf(Screen.HOME)
        private set
    var settingsSection by mutableStateOf(SettingsSection.DISPLAY)
    /** 音訊「來源」全螢幕畫面 */
    var showSources by mutableStateOf(false)
    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var bindings by mutableStateOf<Map<String, String>>(loadBindings())
        private set
    var themeMode by mutableStateOf(prefs.themeMode)
        private set
    var dayStart by mutableStateOf(prefs.dayStart)
        private set
    var nightStart by mutableStateOf(prefs.nightStart)
        private set
    var now by mutableStateOf(LocalDateTime.now())
        private set
    var wifi by mutableStateOf(false)
        private set
    var cellular by mutableStateOf(false)
        private set
    var bluetoothOn by mutableStateOf(false)
        private set
    var clock24h by mutableStateOf(prefs.clock24h)
        private set
    var projectionMode by mutableStateOf(prefs.projectionMode)
        private set
    var usbDevices by mutableStateOf<List<UsbInfo>>(emptyList())
        private set
    var ongoingPkgs by mutableStateOf<Set<String>>(emptySet())
        private set
    /** 綁定為 CarPlay／Android Auto 的 App 的常駐通知標題（只用於設定裡的偵測狀態） */
    var projectionTitles by mutableStateOf<Map<String, List<String>>>(emptyMap())
        private set

    val carPlayStatus: ProjectionStatus
        get() = projectionStatus(bindings[Slots.CARPLAY], usbDevices, ongoingPkgs, ::isAppleUsb)
    val androidAutoStatus: ProjectionStatus
        get() = projectionStatus(bindings[Slots.ANDROID_AUTO], usbDevices, ongoingPkgs, ::isAndroidAccessoryUsb)
    val showCarPlay: Boolean get() = showProjection(bindings[Slots.CARPLAY], projectionMode, carPlayStatus)
    val showAndroidAuto: Boolean get() = showProjection(bindings[Slots.ANDROID_AUTO], projectionMode, androidAutoStatus)

    val isNight: Boolean get() = resolveNight(themeMode, now.hour, dayStart, nightStart)

    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            cellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }
        override fun onLost(network: Network) { wifi = false; cellular = false }
    }

    init {
        runCatching { connectivity.registerDefaultNetworkCallback(netCallback) }
        viewModelScope.launch {
            while (true) {
                now = LocalDateTime.now()
                bluetoothOn = runCatching {
                    app.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
                }.getOrDefault(false)
                refreshUsb()
                delay(5_000)
            }
        }
        viewModelScope.launch { ProjectionSignals.ongoing.collect { ongoingPkgs = it } }
        viewModelScope.launch { ProjectionSignals.titles.collect { projectionTitles = it } }
        reloadApps()
    }

    private fun loadBindings(): Map<String, String> =
        Slots.all.mapNotNull { k -> prefs.binding(k)?.let { k to it } }.toMap()

    fun reloadApps() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.Default) { repo.load() }
            apps = list
            if (!prefs.defaultsApplied && list.isNotEmpty()) {
                Defaults.guess(list.map { it.pkg to it.label }).forEach { (k, v) ->
                    if (prefs.binding(k) == null) prefs.setBinding(k, v)
                }
                prefs.defaultsApplied = true
                bindings = loadBindings()
            }
        }
    }

    fun onResume() {
        media.refresh()
        refreshUsb()
        reloadApps()
    }

    /** 列出 USB 裝置不需要權限（開啟裝置才需要）。 */
    private fun refreshUsb() {
        usbDevices = runCatching {
            app.getSystemService(UsbManager::class.java)?.deviceList?.values?.map {
                UsbInfo(it.vendorId, it.productId, runCatching { it.productName }.getOrNull() ?: it.deviceName)
            }
        }.getOrNull().orEmpty()
    }

    fun go(s: Screen) { screen = s; showSources = false }

    fun openSettings(section: SettingsSection) {
        settingsSection = section
        go(Screen.SETTINGS)
    }

    fun appFor(key: String): AppEntry? = bindings[key]?.let { pkg -> apps.firstOrNull { it.pkg == pkg } }

    fun bind(key: String, pkg: String?) {
        prefs.setBinding(key, pkg)
        bindings = loadBindings()
    }

    fun updateThemeMode(m: ThemeMode) { prefs.themeMode = m; themeMode = m }
    fun updateDayStart(h: Int) { val v = h.coerceIn(0, 23); prefs.dayStart = v; dayStart = v }
    fun updateNightStart(h: Int) { val v = h.coerceIn(0, 23); prefs.nightStart = v; nightStart = v }
    fun updateClock24h(v: Boolean) { prefs.clock24h = v; clock24h = v }
    fun updateProjectionMode(m: ProjectionMode) { prefs.projectionMode = m; projectionMode = m }

    /** 開啟綁定的 App；電話沒綁定時退回系統撥號。 */
    fun open(key: String) {
        val pkg = bindings[key]
        if (pkg != null) {
            if (repo.launch(pkg)) return
            toast("無法開啟，請重新綁定")
            return
        }
        if (key == Slots.PHONE) {
            startSafely(Intent(Intent.ACTION_DIAL))
            return
        }
        toast("尚未綁定「${Slots.labels[key] ?: key}」，請到 設定 → App 綁定")
    }

    fun launchPackage(pkg: String) {
        if (!repo.launch(pkg)) toast("無法開啟此 App")
    }

    fun startVoice() {
        val voice = Intent(Intent.ACTION_VOICE_COMMAND)
        val assist = Intent(Intent.ACTION_ASSIST)
        if (!startSafely(voice) && !startSafely(assist)) toast("此車機沒有可用的語音助理")
    }

    fun openNotificationAccess() = startSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    fun openSystemSettings() = startSafely(Intent(Settings.ACTION_SETTINGS))
    fun openHomeChooser() = startSafely(Intent(Settings.ACTION_HOME_SETTINGS))

    private fun startSafely(intent: Intent): Boolean =
        runCatching { app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

    private fun toast(msg: String) = Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()

    override fun onCleared() {
        media.stop()
        runCatching { connectivity.unregisterNetworkCallback(netCallback) }
    }
}
