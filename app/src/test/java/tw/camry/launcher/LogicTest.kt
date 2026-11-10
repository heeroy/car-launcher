package tw.camry.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.camry.launcher.data.Defaults
import tw.camry.launcher.data.Slots
import tw.camry.launcher.data.ThemeMode
import tw.camry.launcher.data.formatDuration
import tw.camry.launcher.data.isNightAt
import tw.camry.launcher.data.resolveNight
import tw.camry.launcher.media.NowPlaying
import tw.camry.launcher.media.livePosition

class LogicTest {
    @Test fun nightSchedule_defaultHours() {
        assertTrue(isNightAt(3, 6, 18))
        assertFalse(isNightAt(6, 6, 18))
        assertFalse(isNightAt(17, 6, 18))
        assertTrue(isNightAt(18, 6, 18))
        assertTrue(isNightAt(23, 6, 18))
    }

    @Test fun nightSchedule_wrapsPastMidnight() {
        // 日間 20:00 開始、夜間 04:00 開始（夜班）
        assertTrue(isNightAt(2, 20, 4).not())
        assertTrue(isNightAt(10, 20, 4))
        assertFalse(isNightAt(21, 20, 4))
    }

    @Test fun manualModesIgnoreClock() {
        assertFalse(resolveNight(ThemeMode.DAY, 23, 6, 18))
        assertTrue(resolveNight(ThemeMode.NIGHT, 12, 6, 18))
    }

    @Test fun formatDuration() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("3:05", formatDuration(185_000))
    }

    @Test fun livePosition_advancesOnlyWhilePlaying() {
        val playing = NowPlaying(playing = true, position = 10_000, duration = 100_000, updatedAt = 1_000, speed = 1f)
        assertEquals(15_000, livePosition(playing, 6_000))
        assertEquals(10_000, livePosition(playing.copy(playing = false), 6_000))
        assertEquals(100_000, livePosition(playing, 500_000))
    }

    @Test fun defaults_guessKnownApps() {
        val apps = listOf(
            "com.google.android.apps.maps" to "Maps",
            "com.spotify.music" to "Spotify",
            "com.kkbox.android" to "KKBOX",
            "com.google.android.projection.gearhead" to "Android Auto",
            "com.example.zlink" to "CarPlay",
            "com.example.calc" to "計算機",
        )
        val g = Defaults.guess(apps)
        assertEquals("com.google.android.apps.maps", g[Slots.NAV])
        assertEquals("com.google.android.projection.gearhead", g[Slots.ANDROID_AUTO])
        assertEquals("com.example.zlink", g[Slots.CARPLAY])
        assertEquals(setOf("com.spotify.music", "com.kkbox.android"), setOf(g["music0"], g["music1"]))
        assertNull(g["music2"])
        assertNull(g[Slots.VEHICLE])
    }
}
