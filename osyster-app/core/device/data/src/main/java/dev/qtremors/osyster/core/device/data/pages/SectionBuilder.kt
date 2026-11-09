package dev.qtremors.osyster.core.device.data.pages

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoRow
import dev.qtremors.osyster.core.device.domain.InfoSection
import dev.qtremors.osyster.core.device.domain.PermissionKind
import dev.qtremors.osyster.core.device.domain.RowAction

/** Localized labels for collectors. Device facts themselves are shown as Android reports them. */
internal class DeviceText(private val context: Context) {
    operator fun get(@StringRes id: Int): String = context.getString(id)
    fun format(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)
}

/**
 * Builds one [InfoSection]. A null, blank, or "unknown" value becomes "Not reported" instead of
 * text that looks like a real reading.
 */
internal class SectionBuilder(private val text: DeviceText, private val source: String) {
    private val rows = mutableListOf<InfoRow>()
    private val now = System.currentTimeMillis()

    /** Set when some rows wait on a permission; the page then offers to grant it. */
    var permission: PermissionKind? = null

    /** Rows after the first [count] are folded under "Show more", for long inventories. */
    fun foldAfter(count: Int) {
        for (index in count until rows.size) rows[index] = rows[index].copy(advanced = true)
    }

    fun row(key: String, label: String, value: Availability<String>, action: RowAction) {
        rows += InfoRow(key, label, value, copyable = false, advanced = false, action = action)
    }

    fun needs(kind: PermissionKind): Availability<String> {
        permission = kind
        return Availability.PermissionRequired(kind)
    }

    fun row(
        key: String,
        @StringRes label: Int,
        value: String?,
        advanced: Boolean = false,
        copyable: Boolean = true,
        source: String = this.source
    ) {
        rows += InfoRow(key, text[label], reported(value, source), copyable, advanced)
    }

    fun row(key: String, @StringRes label: Int, value: Availability<String>, advanced: Boolean = false, copyable: Boolean = true) {
        rows += InfoRow(key, text[label], value, copyable, advanced)
    }

    /** For rows whose label is built at runtime, such as "Core 3". */
    fun row(key: String, label: String, value: Availability<String>, advanced: Boolean = false) {
        rows += InfoRow(key, label, value, copyable = true, advanced = advanced)
    }

    fun yesNo(key: String, @StringRes label: Int, value: Boolean, advanced: Boolean = false) {
        row(key, label, text[if (value) R.string.value_yes else R.string.value_no], advanced = advanced, copyable = false)
    }

    fun available(value: String, source: String = this.source): Availability<String> =
        Availability.Available(value, source, now)

    fun reported(value: String?, source: String = this.source): Availability<String> =
        if (value.isNullOrBlank() || value.equals(Build.UNKNOWN, ignoreCase = true)) {
            Availability.Unavailable(NOT_REPORTED, retryable = false)
        } else {
            Availability.Available(value, source, now)
        }

    fun build(key: String, title: String): InfoSection = InfoSection(key, title, rows.toList(), permission)

    companion object {
        const val NOT_REPORTED = "Not reported by the device"
        val restrictedProc: Availability<Nothing> = Availability.Restricted("Android blocks this file for apps")
    }
}

internal inline fun section(
    text: DeviceText,
    key: String,
    @StringRes title: Int,
    source: String,
    block: SectionBuilder.() -> Unit
): InfoSection = SectionBuilder(text, source).apply(block).build(key, text[title])
