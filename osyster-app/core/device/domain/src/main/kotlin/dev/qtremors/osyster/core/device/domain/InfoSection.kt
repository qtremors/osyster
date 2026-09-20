package dev.qtremors.osyster.core.device.domain

/**
 * One labeled fact on a device page, such as "Security patch: 1 August 2026".
 *
 * [key] is stable across releases so search, favorites, and copy actions can refer to it.
 * [label] is already localized by the collector's caller; device facts read from Build may
 * stay as reported.
 */
data class InfoRow(
    val key: String,
    val label: String,
    val value: Availability<String>,
    /** Values that are long or meant to be pasted elsewhere (kernel, build ID) get a copy action. */
    val copyable: Boolean = true,
    /** Secondary rows are folded under "Advanced" until expanded. */
    val advanced: Boolean = false
)

/** A titled group of rows, drawn as one block of connected rows. */
data class InfoSection(
    val key: String,
    val title: String,
    val rows: List<InfoRow>
) {
    val primaryRows: List<InfoRow> get() = rows.filterNot(InfoRow::advanced)
    val advancedRows: List<InfoRow> get() = rows.filter(InfoRow::advanced)
}
