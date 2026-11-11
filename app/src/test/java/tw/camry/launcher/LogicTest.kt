package tw.camry.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.camry.launcher.data.Defaults
import tw.camry.launcher.data.Slots
import tw.camry.launcher.data.ThemeMode
import tw.camry.launcher.data.ProjectionMode
import tw.camry.launcher.data.ProjectionStatus
import tw.camry.launcher.data.UsbInfo
import tw.camry.launcher.data.formatClock
import tw.camry.launcher.data.formatDuration
import tw.camry.launcher.data.isAndroidAccessoryUsb
import tw.camry.launcher.data.isAppleUsb
import tw.camry.launcher.data.projectionStatus
import tw.camry.launcher.data.showProjection
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

    @Test fun defaults_guessCarLinkAsCarPlay() {
        val g = Defaults.guess(listOf("com.example.calc" to "計算機", "com.example.carlink" to "CarLink"))
        assertEquals("com.example.carlink", g[Slots.CARPLAY])
    }

    @Test fun formatClock_12and24() {
        assertEquals("2:43 PM", formatClock(14, 43, use24h = false))
        assertEquals("12:05 AM", formatClock(0, 5, use24h = false))
        assertEquals("12:00 PM", formatClock(12, 0, use24h = false))
        assertEquals("11:59 AM", formatClock(11, 59, use24h = false))
        assertEquals("14:43", formatClock(14, 43, use24h = true))
        assertEquals("00:05", formatClock(0, 5, use24h = true))
    }

    @Test fun usbMatchers() {
        assertTrue(isAppleUsb(UsbInfo(0x05AC, 0x12A8, "iPhone")))
        assertFalse(isAppleUsb(UsbInfo(0x18D1, 0x2D00, "Pixel")))
        assertTrue(isAndroidAccessoryUsb(UsbInfo(0x18D1, 0x2D01, "Android Accessory")))
        assertFalse(isAndroidAccessoryUsb(UsbInfo(0x18D1, 0x4EE7, "Pixel (MTP)")))
    }

    @Test fun projection_needsBindingAndDetection() {
        val iphone = listOf(UsbInfo(0x05AC, 0x12A8, "iPhone"))
        val none = ProjectionStatus(usb = false, notification = false)

        // 沒綁定：就算偵測到也不顯示
        val unbound = projectionStatus(null, iphone, emptySet(), ::isAppleUsb)
        assertFalse(showProjection(null, ProjectionMode.AUTO, unbound))
        assertFalse(showProjection(null, ProjectionMode.ALWAYS, unbound))

        // 有綁定、沒偵測到：自動模式不顯示，一律顯示模式顯示
        assertFalse(showProjection("carlink", ProjectionMode.AUTO, none))
        assertTrue(showProjection("carlink", ProjectionMode.ALWAYS, none))

        // USB 或常駐通知其中一個成立就算連線
        val byUsb = projectionStatus("carlink", iphone, emptySet(), ::isAppleUsb)
        assertTrue(byUsb.usb && !byUsb.notification)
        assertTrue(showProjection("carlink", ProjectionMode.AUTO, byUsb))
        val byNote = projectionStatus("carlink", emptyList(), setOf("carlink", "other"), ::isAppleUsb)
        assertTrue(!byNote.usb && byNote.notification)
        assertTrue(showProjection("carlink", ProjectionMode.AUTO, byNote))

        // 別的 App 的常駐通知不算
        assertFalse(projectionStatus("carlink", emptyList(), setOf("other"), ::isAppleUsb).connected)
    }
}
