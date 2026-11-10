package dev.qtremors.osyster.feature.musicwidgets

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager

/**
 * Which apps the music widgets follow. By default any app playing media; or only the apps the
 * person picked, so a video, podcast, or game in the background never takes over the widget.
 * Apps seen playing are remembered so they can be offered in the picker.
 */
internal object MusicApps {
    private const val FILE = "music_apps"
    private const val KEY_ONLY = "only_chosen"
    private const val KEY_CHOSEN = "chosen"
    private const val KEY_SEEN = "seen"
    private const val MEDIA_BROWSER = "android.media.browse.MediaBrowserService"

    private fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun onlyChosen(context: Context): Boolean = prefs(context).getBoolean(KEY_ONLY, false)

    fun chosen(context: Context): Set<String> = prefs(context).getStringSet(KEY_CHOSEN, null).orEmpty()

    /** Whether the widgets may show what [packageName] plays. */
    fun allows(context: Context, packageName: String?): Boolean =
        !onlyChosen(context) || (packageName != null && packageName in chosen(context))

    fun setOnlyChosen(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_ONLY, on).apply()
    }

    fun setChosen(context: Context, packageName: String, on: Boolean) {
        val next = chosen(context).toMutableSet().apply { if (on) add(packageName) else remove(packageName) }
        prefs(context).edit().putStringSet(KEY_CHOSEN, next).apply()
    }

    /** Remembers an app that played media; writes only the first time, so it costs nothing after. */
    fun markSeen(context: Context, packageName: String) {
        if (packageName == context.packageName) return
        val seen = prefs(context).getStringSet(KEY_SEEN, null).orEmpty()
        if (packageName !in seen) prefs(context).edit().putStringSet(KEY_SEEN, seen + packageName).apply()
    }

    /**
     * Apps to offer: ones seen playing, ones that declare a media browser (most music and podcast
     * apps), and ones already chosen, sorted by name.
     */
    fun candidates(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        val browsers = runCatching {
            pm.queryIntentServices(Intent(MEDIA_BROWSER), PackageManager.MATCH_ALL).map { it.serviceInfo.packageName }
        }.getOrDefault(emptyList())
        val seen = prefs(context).getStringSet(KEY_SEEN, null).orEmpty()
        return (browsers + seen + chosen(context)).distinct().filter { it != context.packageName }.mapNotNull { packageName ->
            runCatching { packageName to pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }.getOrNull()
        }.sortedBy { it.second.lowercase() }
    }

    fun listen(context: Context, onChange: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == KEY_ONLY || key == KEY_CHOSEN) onChange() }
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun stopListening(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }
}
