package tw.p4.launcher.data

/** 可綁定的位置。key 會存進 SharedPreferences。 */
object Slots {
    const val CARPLAY = "carplay"
    const val ANDROID_AUTO = "androidauto"
    const val NAV = "nav"
    const val PHONE = "phone"
    const val VEHICLE = "vehicle"
    val MUSIC = listOf("music0", "music1", "music2")
    val FAVORITES = listOf("fav0", "fav1", "fav2", "fav3")

    val labels = mapOf(
        CARPLAY to "Apple CarPlay",
        ANDROID_AUTO to "Android Auto",
        NAV to "導航",
        PHONE to "電話",
        VEHICLE to "車輛資訊",
        "music0" to "音訊來源 1",
        "music1" to "音訊來源 2",
        "music2" to "音訊來源 3",
        "fav0" to "常用 App 1",
        "fav1" to "常用 App 2",
        "fav2" to "常用 App 3",
        "fav3" to "常用 App 4",
    )

    val all: List<String> = listOf(CARPLAY, ANDROID_AUTO, NAV, PHONE, VEHICLE) + MUSIC + FAVORITES
}

/**
 * 第一次開啟時，依套件名稱與顯示名稱猜測預設綁定。
 * apps：(套件名稱, 顯示名稱)。猜不到的位置留空，由使用者在設定裡自行綁定。
 */
object Defaults {
    private val navPackages = listOf("com.google.android.apps.maps", "com.waze")
    private val navLabels = listOf("導航王", "導航", "地圖", "maps", "waze", "navi")
    private val musicPackages = listOf("com.spotify.music", "com.google.android.apps.youtube.music")
    private val musicLabels = listOf("kkbox", "spotify", "音樂", "music")
    private val carplayLabels = listOf("carplay", "carlink", "zlink")
    private val phonePackages =listOf("com.google.android.dialer", "com.android.dialer")

    fun guess(apps: List<Pair<String, String>>): Map<String, String> {
        val out = linkedMapOf<String, String>()

        fun firstBy(packages: List<String>, labels: List<String>): String? =
            apps.firstOrNull { it.first in packages }?.first
                ?: apps.firstOrNull { (_, l) -> labels.any { l.contains(it, ignoreCase = true) } }?.first

        // 智乘車機的 CarPlay App 叫「CarLink」
        apps.firstOrNull { (_, l) -> carplayLabels.any { l.contains(it, ignoreCase = true) } }
            ?.let { out[Slots.CARPLAY] = it.first }

        (apps.firstOrNull { it.first == "com.google.android.projection.gearhead" }
            ?: apps.firstOrNull { it.second.equals("Android Auto", ignoreCase = true) })
            ?.let { out[Slots.ANDROID_AUTO] = it.first }

        firstBy(navPackages, navLabels)?.let { out[Slots.NAV] = it }
        firstBy(phonePackages, listOf("電話", "dialer"))?.let { out[Slots.PHONE] = it }

        val music = (apps.filter { it.first in musicPackages } +
            apps.filter { (_, l) -> musicLabels.any { l.contains(it, ignoreCase = true) } })
            .map { it.first }
            .distinct()
            .filter { it != out[Slots.NAV] }
            .take(Slots.MUSIC.size)
        music.forEachIndexed { i, pkg -> out[Slots.MUSIC[i]] = pkg }
        return out
    }
}
