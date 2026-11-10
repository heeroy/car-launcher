package tw.camry.launcher.data

enum class ThemeMode { AUTO, DAY, NIGHT }

/** 自動模式：dayStart（含）到 nightStart（不含）之間為日間。 */
fun isNightAt(hour: Int, dayStart: Int, nightStart: Int): Boolean =
    if (dayStart <= nightStart) hour < dayStart || hour >= nightStart
    else hour in nightStart until dayStart

fun resolveNight(mode: ThemeMode, hour: Int, dayStart: Int, nightStart: Int): Boolean = when (mode) {
    ThemeMode.DAY -> false
    ThemeMode.NIGHT -> true
    ThemeMode.AUTO -> isNightAt(hour, dayStart, nightStart)
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
