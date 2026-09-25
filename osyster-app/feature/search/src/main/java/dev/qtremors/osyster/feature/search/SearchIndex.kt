package dev.qtremors.osyster.feature.search

import java.text.Normalizer
import java.util.Locale

/** Where a search result leads. Mapped to [SearchDestination] by the route. */
sealed interface SearchTarget {
    data class Page(val pageId: String) : SearchTarget
    data class Tool(val toolId: String) : SearchTarget
    data class Setting(val place: SettingsPlace) : SearchTarget
}

enum class SettingsPlace { SETTINGS, ABOUT, PRIVACY, LICENSES }

enum class SearchKind { PAGE, TOOL, SETTING, FACT }

/**
 * One searchable thing. [title] is what the row shows big; [detail] is the supporting line and is
 * matched too, so "android 16" finds the Android version fact through its value.
 */
data class SearchEntry(
    val key: String,
    val kind: SearchKind,
    val title: String,
    val detail: String,
    val target: SearchTarget
)

/**
 * Ranks [entries] against [query]. Every word of the query must appear somewhere in the title or
 * detail; titles that start with the query rank first, then titles containing it, then matches only
 * in the detail. Pages, tools, and settings sort ahead of facts at equal rank, because a page is
 * usually what the person meant. Accents and case are ignored.
 */
fun searchEntries(query: String, entries: List<SearchEntry>, limit: Int = MAX_RESULTS): List<SearchEntry> {
    val needle = fold(query)
    if (needle.isBlank()) return emptyList()
    val words = needle.split(' ').filter(String::isNotBlank)
    return entries.asSequence()
        .mapNotNull { entry ->
            val title = fold(entry.title)
            val detail = fold(entry.detail)
            if (words.any { it !in title && it !in detail }) return@mapNotNull null
            val rank = when {
                title.startsWith(needle) -> 0
                title.contains(needle) -> 1
                words.all(title::contains) -> 2
                else -> 3
            }
            Scored(entry, rank, if (entry.kind == SearchKind.FACT) 1 else 0)
        }
        .sortedWith(compareBy(Scored::rank, Scored::kindOrder, { it.entry.title.length }))
        .take(limit)
        .map(Scored::entry)
        .toList()
}

/** Lowercase, accents stripped, punctuation turned into spaces, runs of spaces collapsed. */
internal fun fold(text: String): String {
    val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
    val builder = StringBuilder(decomposed.length)
    var lastSpace = true
    for (char in decomposed) {
        when {
            Character.getType(char) == Character.NON_SPACING_MARK.toInt() -> Unit
            char.isLetterOrDigit() || char == '.' -> {
                builder.append(char.lowercaseChar())
                lastSpace = false
            }
            !lastSpace -> {
                builder.append(' ')
                lastSpace = true
            }
        }
    }
    return builder.toString().trimEnd().lowercase(Locale.ROOT)
}

private data class Scored(val entry: SearchEntry, val rank: Int, val kindOrder: Int)

private const val MAX_RESULTS = 60
