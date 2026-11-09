package dev.qtremors.osyster.core.device.data.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProcParsersTest {
    @Test
    fun overallAndCoreLinesParse() {
        val overall = ProcParsers.cpuTimes("cpu  100 0 50 800 50 0 0 0 0 0")!!
        assertNull(overall.core)
        assertEquals(150L, overall.busy)
        assertEquals(850L, overall.idle)
        assertEquals(3, ProcParsers.cpuTimes("cpu3 1 2 3 4 5 6 7")!!.core)
        assertNull(ProcParsers.cpuTimes("intr 12345"))
        assertNull(ProcParsers.cpuTimes("cpuX 1 2 3 4"))
    }

    @Test
    fun loadIsDeltaBusyOverDeltaTotal() {
        val before = CpuTimes(null, busy = 100, idle = 900)
        val after = CpuTimes(null, busy = 150, idle = 950)
        assertEquals(50, ProcParsers.loadPercent(before, after))
        assertNull(ProcParsers.loadPercent(before, before))
    }

    @Test
    fun memInfoReadsKilobytes() {
        val values = ProcParsers.memInfo(listOf("MemTotal:        7864320 kB", "SwapFree:  1024 kB", "garbage"))
        assertEquals(7_864_320L, values["MemTotal"])
        assertEquals(1024L, values["SwapFree"])
        assertEquals(2, values.size)
    }

    @Test
    fun cpuListSupportsRangesAndSpaces() {
        assertEquals(listOf(0, 1, 2, 3, 6), ProcParsers.cpuList("0-3 6"))
        assertEquals(listOf(4, 5, 6), ProcParsers.cpuList("4 5 6\n"))
        assertEquals(emptyList<Int>(), ProcParsers.cpuList(""))
    }

    @Test
    fun kernelReleaseAndVulkanVersion() {
        assertEquals(
            "5.10.252-android12-9-00029-g6315c5776365-ab15925896",
            ProcParsers.kernelRelease("Linux version 5.10.252-android12-9-00029-g6315c5776365-ab15925896 (build-user@build-host) #1 SMP PREEMPT")
        )
        assertEquals("1.3.0", ProcParsers.vulkanVersion((1 shl 22) or (3 shl 12)))
        assertNull(ProcParsers.vulkanVersion(0))
    }
}
