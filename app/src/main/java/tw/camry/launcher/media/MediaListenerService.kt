package tw.camry.launcher.media

import android.service.notification.NotificationListenerService

/**
 * 內容為空。只要使用者在「通知存取」中允許本 App，
 * MediaSessionManager.getActiveSessions() 就能用本服務的 ComponentName 取得播放中的媒體。
 */
class MediaListenerService : NotificationListenerService()
