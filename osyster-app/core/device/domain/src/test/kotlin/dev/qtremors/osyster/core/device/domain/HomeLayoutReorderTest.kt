package dev.qtremors.osyster.core.device.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HomeLayoutReorderTest {
    @Test
    fun reorderGroupKeepsOtherGroupsInPlace() {
        val layout = HomeLayout()
        val performance = listOf(DevicePage.BATTERY, DevicePage.PROCESSOR, DevicePage.MEMORY)
        val next = layout.reorderGroup(performance)
        assertEquals(performance, next.pageOrder.filter { it.group == DevicePageGroup.PERFORMANCE })
        assertEquals(layout.pageOrder.filter { it.group != DevicePageGroup.PERFORMANCE }, next.pageOrder.filter { it.group != DevicePageGroup.PERFORMANCE })
        assertEquals(layout.pageOrder.size, next.pageOrder.size)
    }

    @Test
    fun reorderGroupRejectsMixedOrPartialLists() {
        val layout = HomeLayout()
        assertSame(layout, layout.reorderGroup(listOf(DevicePage.BATTERY, DevicePage.PROCESSOR)))
        assertSame(layout, layout.reorderGroup(listOf(DevicePage.BATTERY, DevicePage.SYSTEM, DevicePage.MEMORY)))
    }

    @Test
    fun trendKeepsLatestPoints() {
        val trend = (1..40).fold(emptyList<Float>()) { list, i -> list.pushTrend(i / 100f) }
        assertEquals(TREND_POINTS, trend.size)
        assertEquals(0.40f, trend.last(), 0.0001f)
        assertEquals(trend, trend.pushTrend(null))
    }

    @Test
    fun reorderPinnedNeedsTheSameItems() {
        val a = PinKey.page("battery")
        val b = PinKey.tool("flashlight")
        val layout = HomeLayout(pinned = listOf(a, b))
        assertEquals(listOf(b, a), layout.reorderPinned(listOf(b, a)).pinned)
        assertSame(layout, layout.reorderPinned(listOf(a)))
    }

    @Test
    fun tileOrderRestoresAndAppendsNewTiles() {
        val restored = HeroTile.restoreOrder(listOf("battery", "cpu", "gone"))
        assertEquals(listOf(HeroTile.BATTERY, HeroTile.CPU), restored.take(2))
        assertEquals(HeroTile.entries.toSet(), restored.toSet())
    }

    @Test
    fun hiddenTilesLeaveTheVisibleList() {
        val layout = HomeLayout().setTileVisible(HeroTile.STORAGE, false)
        assertEquals(HeroTile.entries - HeroTile.STORAGE, layout.visibleTiles)
        assertEquals(HeroTile.entries, layout.setTileVisible(HeroTile.STORAGE, true).visibleTiles)
    }

    @Test
    fun reorderTilesNeedsEveryTile() {
        val layout = HomeLayout()
        val reversed = HeroTile.entries.reversed()
        assertEquals(reversed, layout.reorderTiles(reversed).tileOrder)
        assertSame(layout, layout.reorderTiles(listOf(HeroTile.CPU)))
    }
}
