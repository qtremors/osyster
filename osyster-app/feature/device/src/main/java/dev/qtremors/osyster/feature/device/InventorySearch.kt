package dev.qtremors.osyster.feature.device

import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.InfoSection

/** Pages with long lists (codecs, certificates, sensors) get a search field. */
internal val searchablePages: Set<DevicePage> = setOf(DevicePage.MEDIA, DevicePage.CERTIFICATES, DevicePage.SENSORS)

/**
 * Keeps rows whose label or value contains every word of [query], ignoring case. Matching rows
 * are unfolded so results are never hidden behind "Show more"; sections with no match disappear.
 * A blank query returns the sections unchanged.
 */
internal fun filterSections(sections: List<InfoSection>, query: String): List<InfoSection> {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter(String::isNotEmpty)
    if (words.isEmpty()) return sections
    return sections.mapNotNull { section ->
        val matches = section.rows.filter { row ->
            val value = (row.value as? Availability.Available)?.value.orEmpty()
            val haystack = "${row.label} $value ${section.title}".lowercase()
            words.all(haystack::contains)
        }
        if (matches.isEmpty()) null
        else section.copy(rows = matches.map { it.copy(advanced = false) }, permission = null)
    }
}
