package nl.jouwpakket.speedoverlay

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val PREF_NAME = "speed_overlay_prefs"
    private const val KEY_X = "overlay_x"
    private const val KEY_Y = "overlay_y"
    private const val KEY_ALPHA = "overlay_alpha"
    private const val KEY_SCALE = "overlay_scale"
    private const val KEY_UNIT = "overlay_unit"
    private const val KEY_LANGUAGE = "overlay_language"
    private const val KEY_RUNNING = "overlay_running"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun savePosition(context: Context, x: Int, y: Int) {
        prefs(context).edit().putInt(KEY_X, x).putInt(KEY_Y, y).apply()
    }

    fun loadPosition(context: Context): Pair<Int, Int> {
        val p = prefs(context)
        return p.getInt(KEY_X, 0) to p.getInt(KEY_Y, 0)
    }

    fun saveAlpha(context: Context, alpha: Int) {
        prefs(context).edit().putInt(KEY_ALPHA, alpha).apply()
    }

    fun loadAlpha(context: Context): Int = prefs(context).getInt(KEY_ALPHA, 255)

    fun saveScale(context: Context, scale: Float) {
        prefs(context).edit().putFloat(KEY_SCALE, scale).apply()
    }

    fun loadScale(context: Context): Float = prefs(context).getFloat(KEY_SCALE, 1f)

    fun saveUnit(context: Context, unit: SpeedUnit) {
        prefs(context).edit().putString(KEY_UNIT, unit.name).apply()
    }

    fun loadUnit(context: Context): SpeedUnit =
        runCatching { SpeedUnit.valueOf(prefs(context).getString(KEY_UNIT, SpeedUnit.KMH.name)!!) }
            .getOrDefault(SpeedUnit.KMH)

    fun saveLanguage(context: Context, code: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, code).apply()
    }

    fun loadLanguage(context: Context): String = prefs(context).getString(KEY_LANGUAGE, "nl") ?: "nl"

    fun saveRunning(context: Context, running: Boolean) {
        prefs(context).edit().putBoolean(KEY_RUNNING, running).apply()
    }

    fun isRunning(context: Context): Boolean = prefs(context).getBoolean(KEY_RUNNING, false)
}

enum class SpeedUnit { KMH, MPH }
