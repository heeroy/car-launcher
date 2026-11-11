package tw.camry.launcher.media

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import tw.camry.launcher.data.Prefs
import tw.camry.launcher.data.ProjectionSignals
import tw.camry.launcher.data.Slots

/**
 * 通知存取服務，兩個用途：
 * 1. 讓 MediaSessionManager.getActiveSessions() 能用本服務的 ComponentName 取得播放中的媒體。
 * 2. 偵測 CarPlay／Android Auto 是否連線：只看「哪些 App 掛著常駐通知」（套件名稱），
 *    通知標題只讀綁定為 CarPlay／Android Auto 的那兩個 App，其他 App 的通知內容一律不讀。
 */
class MediaListenerService : NotificationListenerService() {

    override fun onListenerConnected() = publish()
    override fun onListenerDisconnected() = ProjectionSignals.clear()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    private fun publish() {
        val active = runCatching { activeNotifications?.toList() }.getOrNull().orEmpty()
        val ongoing = active.filter { it.isOngoingLike() }
        val prefs = Prefs(this)
        val watched = setOfNotNull(prefs.binding(Slots.CARPLAY), prefs.binding(Slots.ANDROID_AUTO))
        val titles = ongoing
            .filter { it.packageName in watched }
            .groupBy({ it.packageName }) { sbn ->
                val extras = sbn.notification.extras
                listOfNotNull(
                    extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                    extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "（無標題）" }
            }
        ProjectionSignals.publish(ongoing.map { it.packageName }.toSet(), titles)
    }

    private fun StatusBarNotification.isOngoingLike(): Boolean =
        isOngoing || (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
}
