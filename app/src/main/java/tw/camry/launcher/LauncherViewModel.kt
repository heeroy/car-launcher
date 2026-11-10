package tw.camry.launcher

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Intent
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
import tw.camry.launcher.data.Slots
import tw.camry.launcher.data.ThemeMode
import tw.camry.launcher.data.resolveNight
import tw.camry.launcher.media.MediaMonitor
import java.time.LocalDateTime

enum class Screen(val title: String) {
    HOME("主頁"), AUDIO("音訊"), APPS("應用程式"), SETTINGS("設定")
}

class LauncherViewModel(private val app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val repo = AppRepository(app)
    val media = MediaMonitor(app)

    var screen by mutableStateOf(Screen.HOME)
        private set
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
                delay(5_000)
            }
        }
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
        reloadApps()
    }

    fun go(s: Screen) { screen = s }

    fun appFor(key: String): AppEntry? = bindings[key]?.let { pkg -> apps.firstOrNull { it.pkg == pkg } }

    fun bind(key: String, pkg: String?) {
        prefs.setBinding(key, pkg)
        bindings = loadBindings()
    }

    fun updateThemeMode(m: ThemeMode) { prefs.themeMode = m; themeMode = m }
    fun updateDayStart(h: Int) { val v = h.coerceIn(0, 23); prefs.dayStart = v; dayStart = v }
    fun updateNightStart(h: Int) { val v = h.coerceIn(0, 23); prefs.nightStart = v; nightStart = v }

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
