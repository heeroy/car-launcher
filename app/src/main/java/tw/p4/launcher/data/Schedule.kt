package tw.p4.launcher.data

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

/** 狀態列時間：12 小時制為「2:43 PM」（與實車相同），24 小時制為「14:43」。 */
fun formatClock(hour: Int, minute: Int, use24h: Boolean): String =
    if (use24h) "%02d:%02d".format(hour, minute)
    else "%d:%02d %s".format(if (hour % 12 == 0) 12 else hour % 12, minute, if (hour < 12) "AM" else "PM")
