package tw.camry.launcher.data

import android.content.Context
import androidx.core.content.edit

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("camry_launcher", Context.MODE_PRIVATE)

    fun binding(key: String): String? = sp.getString("bind_$key", null)
    fun setBinding(key: String, pkg: String?) = sp.edit { if (pkg == null) remove("bind_$key") else putString("bind_$key", pkg) }

    var defaultsApplied: Boolean
        get() = sp.getBoolean("defaults_applied", false)
        set(v) = sp.edit { putBoolean("defaults_applied", v) }

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(sp.getString("theme_mode", "AUTO")!!) }.getOrDefault(ThemeMode.AUTO)
        set(v) = sp.edit { putString("theme_mode", v.name) }

    var dayStart: Int
        get() = sp.getInt("day_start", 6)
        set(v) = sp.edit { putInt("day_start", v) }

    var nightStart: Int
        get() = sp.getInt("night_start", 18)
        set(v) = sp.edit { putInt("night_start", v) }

    var clock24h: Boolean
        get() = sp.getBoolean("clock_24h", false)
        set(v) = sp.edit { putBoolean("clock_24h", v) }

    var projectionMode: ProjectionMode
        get() = runCatching { ProjectionMode.valueOf(sp.getString("projection_mode", "AUTO")!!) }.getOrDefault(ProjectionMode.AUTO)
        set(v) = sp.edit { putString("projection_mode", v.name) }
}
