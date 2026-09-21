package dev.qtremors.osyster.core.device.domain

import kotlinx.coroutines.flow.Flow

/**
 * A pinned shortcut on Home. Device pages and tools share one pinned list, so each key says
 * which kind it is: "page:system" or "tool:data_usage".
 */
data class PinKey(val kind: Kind, val id: String) {
    enum class Kind(val prefix: String) { PAGE("page"), TOOL("tool") }

    val raw: String get() = "${kind.prefix}:$id"

    companion object {
        fun page(id: String) = PinKey(Kind.PAGE, id)
        fun tool(id: String) = PinKey(Kind.TOOL, id)

        fun parse(raw: String): PinKey? {
            val prefix = raw.substringBefore(':', missingDelimiterValue = "")
            val id = raw.substringAfter(':', missingDelimiterValue = "")
            val kind = Kind.entries.firstOrNull { it.prefix == prefix } ?: return null
            return if (id.isBlank()) null else PinKey(kind, id)
        }
    }
}

/** How the user arranged Home: device page order, hidden pages, and pinned shortcuts. */
data class HomeLayout(
    val pageOrder: List<DevicePage> = DevicePage.entries,
    val hiddenPages: Set<DevicePage> = emptySet(),
    val pinned: List<PinKey> = emptyList()
) {
    val visiblePages: List<DevicePage> get() = pageOrder.filterNot(hiddenPages::contains)

    fun isPinned(key: PinKey): Boolean = key in pinned

    fun togglePin(key: PinKey): HomeLayout =
        copy(pinned = if (key in pinned) pinned - key else pinned + key)

    /** Moves a page one step within its own group, so groups stay together. */
    fun move(page: DevicePage, up: Boolean): HomeLayout {
        val siblings = pageOrder.filter { it.group == page.group && it !in hiddenPages }
        val index = siblings.indexOf(page)
        val target = siblings.getOrNull(if (up) index - 1 else index + 1) ?: return this
        val order = pageOrder.toMutableList()
        val from = order.indexOf(page)
        val to = order.indexOf(target)
        order[from] = target
        order[to] = page
        return copy(pageOrder = order)
    }

    fun hide(page: DevicePage): HomeLayout = copy(
        hiddenPages = hiddenPages + page,
        pinned = pinned - PinKey.page(page.id)
    )

    fun show(page: DevicePage): HomeLayout = copy(hiddenPages = hiddenPages - page)
}

/** Read and change the Home arrangement. Persisted; survives updates that add new pages. */
interface HomeLayoutRepository {
    val layout: Flow<HomeLayout>
    suspend fun update(transform: (HomeLayout) -> HomeLayout)
}
