package tw.p4.launcher.media

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/**
 * 音訊控制：送出系統媒體按鍵（與方向盤按鍵同一條路），由目前播放中的 App 處理。
 * 不讀取播放資訊，所以不需要「通知存取」。
 */
class MediaKeys(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)

    private fun send(code: Int) {
        val t = SystemClock.uptimeMillis()
        runCatching {
            audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0))
            audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0))
        }
    }

    fun playPause() = send(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun next() = send(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous() = send(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
}
