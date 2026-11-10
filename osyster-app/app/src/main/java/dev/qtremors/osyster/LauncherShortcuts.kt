package dev.qtremors.osyster

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dev.qtremors.osyster.core.navigation.DeepLinks
import dev.qtremors.osyster.core.navigation.Tool

/**
 * Long-press the launcher icon to jump straight into the tools people open most. Published once at
 * start; the launcher keeps them, and updating replaces the same ids, so no stale copies pile up.
 */
internal object LauncherShortcuts {
    // Most launchers show the first four or five, so the ones people reach for most lead.
    private val tools = listOf(
        Shortcut(Tool.DATA_USAGE, R.string.shortcut_data_usage, R.drawable.ic_shortcut_data),
        Shortcut(Tool.BARCODE, R.string.shortcut_scan, R.drawable.ic_shortcut_scan),
        Shortcut(Tool.BARCODE, R.string.shortcut_create_code, R.drawable.ic_shortcut_create, DeepLinks.PAGE_CREATE_CODE),
        Shortcut(Tool.FLASHLIGHT, R.string.shortcut_flashlight, R.drawable.ic_shortcut_flashlight),
        Shortcut(Tool.STAY_AWAKE, R.string.shortcut_stay_awake, R.drawable.ic_shortcut_coffee),
        Shortcut(Tool.SCREEN_USAGE, R.string.shortcut_screen_usage, R.drawable.ic_shortcut_hourglass),
        Shortcut(Tool.BARCODE, R.string.shortcut_scan_picture, R.drawable.ic_shortcut_picture, DeepLinks.PAGE_SCAN_PICTURE)
    )

    fun publish(context: Context) {
        val limit = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtLeast(1)
        val shortcuts = tools.take(limit).mapIndexed { rank, shortcut -> shortcut.build(context, rank) }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    private data class Shortcut(val tool: Tool, val label: Int, val icon: Int, val page: String? = null) {
        fun build(context: Context, rank: Int): ShortcutInfoCompat {
            val intent = Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(DeepLinks.EXTRA_OPEN_TOOL, tool.id)
            page?.let { intent.putExtra(DeepLinks.EXTRA_OPEN_PAGE, it) }
            return ShortcutInfoCompat.Builder(context, "tool_${tool.id}" + page?.let { "_$it" }.orEmpty())
                .setShortLabel(context.getString(label))
                .setIcon(IconCompat.createWithResource(context, icon))
                .setIntent(intent)
                .setRank(rank)
                .build()
        }
    }
}
