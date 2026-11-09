package dev.qtremors.osyster.core.privilege.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandTest {
    @Test
    fun plainWordsStayBare() {
        assertEquals("am force-stop com.example.app", PowerCommands.forceStop("com.example.app").text)
    }

    @Test
    fun valuesWithSpacesAndQuotesStayOneArgument() {
        val command = PowerCommands.putSetting(SettingsTable.GLOBAL, "battery_saver_constants", "a=1, b='x'; rm -rf /")
        assertEquals("settings put global battery_saver_constants 'a=1, b='\\''x'\\''; rm -rf /'", command.text)
    }

    @Test
    fun emptyArgumentIsQuoted() {
        assertEquals("settings put secure icon_blacklist ''", PowerCommands.putSetting(SettingsTable.SECURE, "icon_blacklist", "").text)
    }

    @Test
    fun invalidPackageNamesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { PowerCommands.freeze("com.example; reboot") }
        assertThrows(IllegalArgumentException::class.java) { PowerCommands.freeze("noDots") }
        assertTrue(PackageNames.isValid("com.android.chrome"))
        assertFalse(PackageNames.isValid("1com.bad"))
    }

    @Test
    fun invalidSettingKeysAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { SettingKey(SettingsTable.GLOBAL, "adb_enabled;id") }
        assertThrows(IllegalArgumentException::class.java) { PowerCommands.deleteSetting(SettingsTable.GLOBAL, "Bad Key") }
    }

    @Test
    fun densityIsBounded() {
        assertEquals("wm density 480", PowerCommands.setDensity(480).text)
        assertThrows(IllegalArgumentException::class.java) { PowerCommands.setDensity(20) }
    }

    @Test
    fun readOnlyBucketsCannotBeSet() {
        assertEquals("am set-standby-bucket com.a.b rare", PowerCommands.setStandbyBucket("com.a.b", StandbyBucket.RARE).text)
        assertThrows(IllegalArgumentException::class.java) { PowerCommands.setStandbyBucket("com.a.b", StandbyBucket.EXEMPTED) }
    }
}

class PowerParsersTest {
    @Test
    fun parsesPackageList() {
        val output = "package:com.example.one\npackage:com.example.two\n\nWarning: junk\npackage:bad name"
        assertEquals(setOf("com.example.one", "com.example.two"), parsePackageList(output))
    }

    @Test
    fun parsesStandbyBuckets() {
        val output = "com.example.one: 10\ncom.example.two: 45\ncom.example.three: 99\nnot a line"
        assertEquals(
            mapOf("com.example.one" to StandbyBucket.ACTIVE, "com.example.two" to StandbyBucket.RESTRICTED),
            parseStandbyBuckets(output)
        )
    }

    @Test
    fun parsesDensityWithAndWithoutOverride() {
        assertEquals(DensityInfo(420, null), parseDensity("Physical density: 420"))
        assertEquals(480, parseDensity("Physical density: 420\nOverride density: 480")?.current)
        assertNull(parseDensity("error"))
    }
}

class BatterySaverConstantsTest {
    @Test
    fun keepsUnknownKeysWhenEditing() {
        val constants = BatterySaverConstants.parse("vendor_key=7,disable_vibration=false")
        val edited = constants.with(SaverPolicy.DISABLE_ANIMATION, true)
        assertEquals("vendor_key=7,disable_vibration=false,disable_animation=true", edited.format())
    }

    @Test
    fun settingDefaultRemovesKey() {
        val constants = BatterySaverConstants.parse("disable_vibration=false")
        assertNull(constants.with(SaverPolicy.DISABLE_VIBRATION, true).format())
    }

    @Test
    fun missingKeyReadsSystemDefault() {
        val constants = BatterySaverConstants.parse(null)
        assertTrue(constants.flag(SaverPolicy.DISABLE_VIBRATION))
        assertFalse(constants.flag(SaverPolicy.DISABLE_ANIMATION))
    }

    @Test
    fun ignoresMalformedPairs() {
        assertEquals("a=1", BatterySaverConstants.parse("a=1,=2,b=,c").format())
    }
}

class StatusAndDnsTest {
    @Test
    fun iconBlacklistKeepsUnknownSlots() {
        val list = IconBlacklist.parse("vendor_icon, alarm_clock")
        assertTrue(list.isHidden(StatusIcon.ALARM))
        assertEquals("nfc,vendor_icon", list.with(StatusIcon.ALARM, false).with(StatusIcon.NFC, true).format())
        assertNull(IconBlacklist.parse("alarm_clock").with(StatusIcon.ALARM, false).format())
    }

    @Test
    fun privateDnsCyclesThroughSavedHost() {
        assertEquals(PrivateDnsMode.AUTOMATIC, nextPrivateDnsMode(PrivateDnsMode.OFF, hasHost = true))
        assertEquals(PrivateDnsMode.HOSTNAME, nextPrivateDnsMode(PrivateDnsMode.AUTOMATIC, hasHost = true))
        assertEquals(PrivateDnsMode.OFF, nextPrivateDnsMode(PrivateDnsMode.AUTOMATIC, hasHost = false))
        assertEquals(PrivateDnsMode.AUTOMATIC, PrivateDnsMode.parse(null))
    }

    @Test
    fun dnsHostValidation() {
        assertTrue(isValidDnsHost("dns.adguard.com"))
        assertFalse(isValidDnsHost("not a host"))
        assertFalse(isValidDnsHost("-bad.com"))
    }

    @Test
    fun debugSnapshotRoundTrips() {
        val snapshot = DebugSnapshot(developerOptions = true, usbDebugging = false, wirelessDebugging = true)
        assertEquals(snapshot, DebugSnapshot.decode(snapshot.encode()))
        assertNull(DebugSnapshot.decode("1,0"))
    }

    @Test
    fun statusShellRoutePrefersShizuku() {
        assertEquals(ShellRoute.SHIZUKU, PrivilegeStatus(shizuku = ShizukuState.READY, root = true).shellRoute)
        assertEquals(ShellRoute.ROOT, PrivilegeStatus(root = true).shellRoute)
        assertFalse(PrivilegeStatus().canWriteSettings)
        assertTrue(PrivilegeStatus(secureSettings = true).canWriteSettings)
    }
}
