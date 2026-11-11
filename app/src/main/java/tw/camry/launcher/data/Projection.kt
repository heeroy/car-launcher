package tw.camry.launcher.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 左側選單的 CarPlay／Android Auto 圖示：自動偵測連線，或只要有綁定就一律顯示。 */
enum class ProjectionMode { AUTO, ALWAYS }

/** 一個 USB 裝置的簡要資訊（只取不需要權限就能讀的欄位）。 */
data class UsbInfo(val vendorId: Int, val productId: Int, val name: String)

const val APPLE_VENDOR_ID = 0x05AC
const val GOOGLE_VENDOR_ID = 0x18D1

/** 有線 CarPlay：USB 上有 Apple 裝置。 */
fun isAppleUsb(d: UsbInfo): Boolean = d.vendorId == APPLE_VENDOR_ID

/**
 * 有線 Android Auto：手機被車機 App 切換成 Android Open Accessory 模式時，
 * 會以 Google 的 vendor ID 與 0x2D00–0x2D05 的 product ID 出現。
 */
fun isAndroidAccessoryUsb(d: UsbInfo): Boolean = d.vendorId == GOOGLE_VENDOR_ID && d.productId in 0x2D00..0x2D05

/** 偵測結果：哪個訊號成立。 */
data class ProjectionStatus(val usb: Boolean, val notification: Boolean) {
    val connected: Boolean get() = usb || notification
}

fun projectionStatus(
    boundPkg: String?,
    usbDevices: List<UsbInfo>,
    ongoingPkgs: Set<String>,
    usbMatch: (UsbInfo) -> Boolean,
): ProjectionStatus = ProjectionStatus(
    usb = usbDevices.any(usbMatch),
    notification = boundPkg != null && boundPkg in ongoingPkgs,
)

/** 有綁定才可能顯示；自動模式下還要偵測到連線。 */
fun showProjection(boundPkg: String?, mode: ProjectionMode, status: ProjectionStatus): Boolean =
    boundPkg != null && (mode == ProjectionMode.ALWAYS || status.connected)

/**
 * 由通知存取服務更新：目前掛著「常駐通知」的 App，以及（只限綁定為 CarPlay／Android Auto 的 App）通知標題。
 * 通知標題只放在記憶體、只顯示在設定的偵測狀態裡，方便確認哪個通知代表「已連線」。
 */
object ProjectionSignals {
    private val _ongoing = MutableStateFlow<Set<String>>(emptySet())
    val ongoing: StateFlow<Set<String>> = _ongoing

    private val _titles = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val titles: StateFlow<Map<String, List<String>>> = _titles

    fun publish(ongoing: Set<String>, titles: Map<String, List<String>>) {
        _ongoing.value = ongoing
        _titles.value = titles
    }

    fun clear() = publish(emptySet(), emptyMap())
}
