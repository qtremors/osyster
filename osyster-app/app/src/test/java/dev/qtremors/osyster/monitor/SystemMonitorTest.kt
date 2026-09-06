package dev.qtremors.osyster.monitor

import dev.qtremors.osyster.settings.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemMonitorTest {
    private lateinit var previousLocale: java.util.Locale

    @org.junit.Before
    fun setTestLocale() {
        previousLocale = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.US)
    }

    @org.junit.After
    fun restoreLocale() { java.util.Locale.setDefault(previousLocale) }


    @Test
    fun telemetryResult_available_returnsValueAndFlags() {
        val result: TelemetryResult<Float> = TelemetryResult.Available(42.5f)

        assertTrue(result.isAvailable)
        assertFalse(result.isRestricted)
        assertEquals(42.5f, result.getOrNull())
        assertEquals(42.5f, result.getOrDefault(0f), 0.001f)
    }

    @Test
    fun telemetryResult_restricted_returnsNullAndFlags() {
        val result: TelemetryResult<Float> = TelemetryResult.Restricted

        assertFalse(result.isAvailable)
        assertTrue(result.isRestricted)
        assertNull(result.getOrNull())
        assertEquals(0f, result.getOrDefault(0f), 0.001f)
    }

    @Test
    fun cpuState_restrictedFlags_reflectTelemetry() {
        val state = CpuState(
            overallUsage = TelemetryResult.Restricted,
            coreStates = emptyList(),
            cpuTempCelsius = TelemetryResult.Restricted,
            cpuModel = "ARMv8",
            cpuArchitecture = "aarch64"
        )

        assertTrue(state.isUsageRestricted)
        assertTrue(state.isTempRestricted)
        assertEquals(0f, state.overallUsageOrZero, 0.001f)
    }

    @Test
    fun resolveUid_mapsCommonSystemUids() {
        val cache = mutableMapOf<Int, String>()

        assertEquals("root", SystemMonitor.resolveUid(0, null, cache))
        assertEquals("system", SystemMonitor.resolveUid(1000, null, cache))
        assertEquals("radio", SystemMonitor.resolveUid(1001, null, cache))
        assertEquals("bluetooth", SystemMonitor.resolveUid(1002, null, cache))
        assertEquals("wifi", SystemMonitor.resolveUid(1010, null, cache))
        assertEquals("media", SystemMonitor.resolveUid(1013, null, cache))
        assertEquals("audioserver", SystemMonitor.resolveUid(1028, null, cache))
        assertEquals("cameraserver", SystemMonitor.resolveUid(1037, null, cache))
        assertEquals("shell", SystemMonitor.resolveUid(2000, null, cache))
        assertEquals("nobody", SystemMonitor.resolveUid(9999, null, cache))
    }

    @Test
    fun resolveUid_mapsStandardAppUids() {
        val cache = mutableMapOf<Int, String>()

        // Primary user app UID 10123 -> u0_a123
        assertEquals("u0_a123", SystemMonitor.resolveUid(10123, null, cache))
        // Secondary user (profile 10) app UID 1010045 -> u10_a45
        assertEquals("u10_a45", SystemMonitor.resolveUid(1010045, null, cache))
    }

    @Test
    fun resolveUid_usesCacheCorrectly() {
        val cache = mutableMapOf<Int, String>()
        cache[10250] = "custom.cached.app"

        assertEquals("custom.cached.app", SystemMonitor.resolveUid(10250, null, cache))
    }

    @Test
    fun temperatureUnit_convertsAndFormatsCorrectly() {
        val c = TemperatureUnit.CELSIUS
        val f = TemperatureUnit.FAHRENHEIT

        assertEquals(25.0f, c.convertFromCelsius(25.0f), 0.001f)
        assertEquals(77.0f, f.convertFromCelsius(25.0f), 0.001f)
        assertEquals(32.0f, f.convertFromCelsius(0.0f), 0.001f)
        assertEquals(212.0f, f.convertFromCelsius(100.0f), 0.001f)

        assertEquals("25.0 °C", c.format(25.0f))
        assertEquals("77.0 °F", f.format(25.0f))
    }

    @Test
    fun parseProcStatLine_standardOverallLine_returnsValidSnapshot() {
        // Line format: cpu user nice system idle iowait irq softirq
        val line = "cpu  101459 1234 45678 890123 456 789 123 0 0 0"
        val snapshot = SystemMonitor.parseProcStatLine(line)

        assertTrue(snapshot != null)
        assertNull(snapshot!!.coreId)
        // active = user(101459) + nice(1234) + system(45678) + irq(789) + softirq(123) = 149283
        assertEquals(149283L, snapshot.activeTime)
        // idle = idle(890123) + iowait(456) = 890579
        assertEquals(890579L, snapshot.idleTime)
    }

    @Test
    fun parseProcStatLine_standardCoreLine_returnsCoreIdAndTimes() {
        val line = "cpu3 25000 100 8000 150000 50 10 5 0 0 0"
        val snapshot = SystemMonitor.parseProcStatLine(line)

        assertTrue(snapshot != null)
        assertEquals(3, snapshot!!.coreId)
        // active = 25000 + 100 + 8000 + 10 + 5 = 33115
        assertEquals(33115L, snapshot.activeTime)
        // idle = 150000 + 50 = 150050
        assertEquals(150050L, snapshot.idleTime)
    }

    @Test
    fun parseProcStatLine_malformedOrIrrelevantLines_returnsNull() {
        assertNull(SystemMonitor.parseProcStatLine(""))
        assertNull(SystemMonitor.parseProcStatLine("   "))
        assertNull(SystemMonitor.parseProcStatLine("intr 123456 789"))
        assertNull(SystemMonitor.parseProcStatLine("ctxt 987654"))
        assertNull(SystemMonitor.parseProcStatLine("btime 1600000000"))
        assertNull(SystemMonitor.parseProcStatLine("processes 5432"))
        assertNull(SystemMonitor.parseProcStatLine("cpu")) // too short
        assertNull(SystemMonitor.parseProcStatLine("cpu 100 200")) // fewer than 5 columns
        assertNull(SystemMonitor.parseProcStatLine("cpux 100 200 300 400 500")) // invalid core id
    }

    @Test
    fun calculateCpuUsage_validDeltas_computesCorrectPercentage() {
        // 25 active out of 100 total -> 25%
        assertEquals(25.0f, SystemMonitor.calculateCpuUsage(25L, 100L), 0.01f)
        // 50 active out of 50 total -> 100%
        assertEquals(100.0f, SystemMonitor.calculateCpuUsage(50L, 50L), 0.01f)
        // 0 active out of 100 total -> 0%
        assertEquals(0.0f, SystemMonitor.calculateCpuUsage(0L, 100L), 0.01f)
    }

    @Test
    fun calculateCpuUsage_zeroOrNegativeDeltas_returnsZero() {
        assertEquals(0.0f, SystemMonitor.calculateCpuUsage(0L, 0L), 0.001f)
        assertEquals(0.0f, SystemMonitor.calculateCpuUsage(-5L, 100L), 0.001f)
        assertEquals(0.0f, SystemMonitor.calculateCpuUsage(10L, -50L), 0.001f)
    }

    @Test
    fun parseMemInfo_standardMemInfo_parsesAllFields() {
        val meminfoContent = """
            MemTotal:        8042496 kB
            MemFree:          524288 kB
            MemAvailable:    3145728 kB
            Buffers:          131072 kB
            Cached:          1048576 kB
            SwapTotal:       2097152 kB
            SwapFree:        1572864 kB
        """.trimIndent()

        val state = SystemMonitor.parseMemInfo(meminfoContent.lineSequence())

        assertEquals(8042496L, state.ramTotalKb)
        assertEquals(524288L, state.ramFreeKb)
        assertEquals(3145728L, state.ramAvailableKb)
        // ramUsed = MemTotal - MemAvailable = 8042496 - 3145728 = 4896768
        assertEquals(4896768L, state.ramUsedKb)
        assertEquals(131072L, state.ramBuffersKb)
        assertEquals(1048576L, state.ramCachedKb)
        assertEquals(2097152L, state.swapTotalKb)
        assertEquals(1572864L, state.swapFreeKb)
        // swapUsed = SwapTotal - SwapFree = 2097152 - 1572864 = 524288
        assertEquals(524288L, state.swapUsedKb)
    }

    @Test
    fun parseMemInfo_missingMemAvailable_estimatesFromFreeAndBuffersAndCached() {
        val meminfoContent = """
            MemTotal:        4000000 kB
            MemFree:         1000000 kB
            Buffers:          200000 kB
            Cached:           800000 kB
            SwapTotal:             0 kB
            SwapFree:              0 kB
        """.trimIndent()

        val state = SystemMonitor.parseMemInfo(meminfoContent.lineSequence())

        // MemAvailable estimated = MemFree (1000000) + Buffers (200000) + Cached (800000) = 2000000
        assertEquals(2000000L, state.ramAvailableKb)
        // ramUsed = 4000000 - 2000000 = 2000000
        assertEquals(2000000L, state.ramUsedKb)
        assertEquals(0L, state.swapTotalKb)
        assertEquals(0L, state.swapUsedKb)
    }

    @Test
    fun parseMemInfo_emptyOrMalformed_returnsDefaultZeroedState() {
        val emptyState = SystemMonitor.parseMemInfo(emptySequence())
        assertEquals(0L, emptyState.ramTotalKb)
        assertEquals(0L, emptyState.ramUsedKb)
        assertEquals(0L, emptyState.ramAvailableKb)

        val malformedState = SystemMonitor.parseMemInfo(sequenceOf("InvalidLineWithoutColon", "Corrupt: abc kB"))
        assertEquals(0L, malformedState.ramTotalKb)
        assertEquals(0L, malformedState.ramUsedKb)
    }

    @Test
    fun parseStatusUid_standardTabbedUid_returnsUid() {
        val statusLines = sequenceOf(
            "Name:\tcom.example.app",
            "State:\tS (sleeping)",
            "Tgid:\t1234",
            "Pid:\t1234",
            "PPid:\t567",
            "Uid:\t10234\t10234\t10234\t10234",
            "Gid:\t10234\t10234\t10234\t10234"
        )
        assertEquals(10234, SystemMonitor.parseStatusUid(statusLines))
    }

    @Test
    fun parseStatusUid_spacedUid_returnsUid() {
        val statusLines = sequenceOf(
            "Name: system_server",
            "Uid: 1000 1000 1000 1000"
        )
        assertEquals(1000, SystemMonitor.parseStatusUid(statusLines))
    }

    @Test
    fun parseStatusUid_missingOrMalformed_returnsNull() {
        val noUid = sequenceOf("Name: proc", "Pid: 123")
        assertNull(SystemMonitor.parseStatusUid(noUid))

        val malformedUid = sequenceOf("Uid: not_a_number")
        assertNull(SystemMonitor.parseStatusUid(malformedUid))

        assertNull(SystemMonitor.parseStatusUid(emptySequence()))
    }

    @Test
    fun parseRssFromStatm_standardStatm_calculatesKb() {
        // Format: size resident shared text lib data dirty
        // 2nd column is resident pages. 1000 pages * 4096 bytes / 1024 = 4000 KB
        val statm = "5000 1000 200 50 0 400 0"
        assertEquals(4000L, SystemMonitor.parseRssFromStatm(statm, 4096L))
    }

    @Test
    fun parseRssFromStatm_malformedOrEmpty_returnsZero() {
        assertEquals(0L, SystemMonitor.parseRssFromStatm(""))
        assertEquals(0L, SystemMonitor.parseRssFromStatm("   "))
        assertEquals(0L, SystemMonitor.parseRssFromStatm("only_one_token"))
        assertEquals(0L, SystemMonitor.parseRssFromStatm("size non_numeric"))
    }

    @Test
    fun parseThermalTemp_milliCelsius_convertsToCelsius() {
        val result = SystemMonitor.parseThermalTemp("42500\n")
        assertTrue(result.isAvailable)
        assertEquals(42.5f, (result as TelemetryResult.Available).value, 0.01f)
    }

    @Test
    fun parseThermalTemp_directCelsius_returnsAvailable() {
        val result = SystemMonitor.parseThermalTemp("38.2")
        assertTrue(result.isAvailable)
        assertEquals(38.2f, (result as TelemetryResult.Available).value, 0.01f)
    }

    @Test
    fun parseThermalTemp_outOfRangeOrInvalid_returnsRestricted() {
        // Below 10 C or above 105 C is considered out of plausible range / invalid
        assertTrue(SystemMonitor.parseThermalTemp("0").isRestricted)
        assertTrue(SystemMonitor.parseThermalTemp("-5000").isRestricted)
        assertTrue(SystemMonitor.parseThermalTemp("150").isRestricted) // > 105
        assertTrue(SystemMonitor.parseThermalTemp("invalid").isRestricted)
        assertTrue(SystemMonitor.parseThermalTemp("").isRestricted)
    }
}
