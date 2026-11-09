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
/** The tiles in Home's dashboard card. [id] is stored, so it never changes once released. */
enum class HeroTile(val id: String) {
    CPU("cpu"),
    RAM("ram"),
    STORAGE("storage"),
    BATTERY("battery"),
    SCREEN_TIME("screen_time"),
    DATA_TODAY("data_today");

    companion object {
        private val byId = entries.associateBy(HeroTile::id)
        fun fromId(id: String): HeroTile? = byId[id]

        /** Restores a saved order: unknown ids are dropped, tiles added in an update are appended. */
        fun restoreOrder(savedIds: List<String>): List<HeroTile> {
            val saved = savedIds.mapNotNull(::fromId).distinct()
            return saved + entries.filterNot(saved::contains)
        }
    }
}

data class HomeLayout(
    val pageOrder: List<DevicePage> = DevicePage.entries,
    val hiddenPages: Set<DevicePage> = emptySet(),
    val pinned: List<PinKey> = emptyList(),
    val tileOrder: List<HeroTile> = HeroTile.entries,
    val hiddenTiles: Set<HeroTile> = emptySet()
) {
    val visiblePages: List<DevicePage> get() = pageOrder.filterNot(hiddenPages::contains)
    val visibleTiles: List<HeroTile> get() = tileOrder.filterNot(hiddenTiles::contains)

    fun reorderTiles(ordered: List<HeroTile>): HomeLayout =
        if (ordered.toSet() != tileOrder.toSet()) this else copy(tileOrder = ordered)

    fun setTileVisible(tile: HeroTile, visible: Boolean): HomeLayout =
        copy(hiddenTiles = if (visible) hiddenTiles - tile else hiddenTiles + tile)

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

    /**
     * Puts [ordered] (the pages of one group, in their new order) back into the full order, each
     * taking the slot a page of that group held before. Other groups do not move.
     */
    fun reorderGroup(ordered: List<DevicePage>): HomeLayout {
        val group = ordered.firstOrNull()?.group ?: return this
        val slots = pageOrder.withIndex().filter { it.value.group == group }.map { it.index }
        if (slots.size != ordered.size || ordered.toSet() != slots.map(pageOrder::get).toSet()) return this
        val order = pageOrder.toMutableList()
        slots.zip(ordered).forEach { (slot, page) -> order[slot] = page }
        return copy(pageOrder = order)
    }

    /** Replaces the pinned order, ignoring anything that is not already pinned. */
    fun reorderPinned(ordered: List<PinKey>): HomeLayout {
        if (ordered.toSet() != pinned.toSet()) return this
        return copy(pinned = ordered)
    }
}

/** Read and change the Home arrangement. Persisted; survives updates that add new pages. */
interface HomeLayoutRepository {
    val layout: Flow<HomeLayout>
    suspend fun update(transform: (HomeLayout) -> HomeLayout)
}
