package tw.p4.launcher.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NowPlaying(
    val hasAccess: Boolean = false,
    val hasSession: Boolean = false,
    val pkg: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val art: ImageBitmap? = null,
    val playing: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    /** PlaybackState.lastPositionUpdateTime（elapsedRealtime 基準） */
    val updatedAt: Long = 0,
    val speed: Float = 1f,
    val canSeek: Boolean = false,
    val canNext: Boolean = false,
    val canPrev: Boolean = false,
)

/** 依更新時間推算目前播放位置。 */
fun livePosition(np: NowPlaying, elapsedNow: Long): Long {
    if (!np.playing || np.updatedAt <= 0) return np.position
    val p = np.position + ((elapsedNow - np.updatedAt) * np.speed).toLong()
    return if (np.duration > 0) p.coerceIn(0, np.duration) else p.coerceAtLeast(0)
}

class MediaMonitor(private val context: Context) {
    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state

    private val msm = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private var controller: MediaController? = null
    private var listening = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = refresh()
    }

    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** 進入前景或使用者回來時呼叫。 */
    fun refresh() {
        if (!hasAccess()) {
            stop()
            _state.value = NowPlaying(hasAccess = false)
            return
        }
        try {
            if (!listening) {
                msm.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
                listening = true
            }
            pick(msm.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            _state.value = NowPlaying(hasAccess = false)
        }
    }

    fun stop() {
        if (listening) {
            runCatching { msm.removeOnActiveSessionsChangedListener(sessionsListener) }
            listening = false
        }
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun pick(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list?.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        if (c == null) {
            _state.value = NowPlaying(hasAccess = true)
            return
        }
        val md = c.metadata
        val ps = c.playbackState
        val actions = ps?.actions ?: 0L
        val art = md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        _state.value = NowPlaying(
            hasAccess = true,
            hasSession = true,
            pkg = c.packageName,
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = (md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty(),
            album = md?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            art = art?.asImageBitmap(),
            playing = ps?.state == PlaybackState.STATE_PLAYING,
            position = ps?.position?.coerceAtLeast(0) ?: 0,
            duration = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0,
            updatedAt = ps?.lastPositionUpdateTime ?: 0,
            speed = ps?.playbackSpeed ?: 1f,
            canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L,
            canNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canPrev = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
        )
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
    fun seekTo(ms: Long) { controller?.transportControls?.seekTo(ms) }
}
