package dev.qtremors.osyster.core.usage.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficTotalsTest {
    private val totals = TrafficTotals(mobileRx = 100, mobileTx = 10, wifiRx = 1_000, wifiTx = 50)

    @Test
    fun filtersPickTheRightCounters() {
        assertEquals(1_160L, totals.total(NetworkFilter.ALL))
        assertEquals(110L, totals.total(NetworkFilter.MOBILE))
        assertEquals(1_050L, totals.total(NetworkFilter.WIFI))
        assertEquals(1_100L, totals.received(NetworkFilter.ALL))
        assertEquals(60L, totals.sent(NetworkFilter.ALL))
    }

    @Test
    fun appsAreFilteredAndSortedPerConnection() {
        val report = DataUsageReport(
            DataPeriod.TODAY, totals, emptyList(),
            listOf(
                AppTraffic("a", "A", TrafficTotals(mobileRx = 5)),
                AppTraffic("b", "B", TrafficTotals(wifiRx = 50)),
                AppTraffic("c", "C", TrafficTotals(mobileRx = 9, wifiRx = 1))
            )
        )
        assertEquals(listOf("c", "a"), report.appsFor(NetworkFilter.MOBILE).map(AppTraffic::packageName))
        assertEquals(listOf("b", "c", "a"), report.appsFor(NetworkFilter.ALL).map(AppTraffic::packageName))
    }
}
