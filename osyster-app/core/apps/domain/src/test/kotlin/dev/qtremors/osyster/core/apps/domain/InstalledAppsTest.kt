package dev.qtremors.osyster.core.apps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledAppsTest {
    private fun app(
        name: String,
        vararg permissions: Pair<String, Boolean>,
        special: Set<SpecialAccess> = emptySet(),
        updated: Long = 0,
        size: Long = 0
    ) = AppSummary(
        packageName = "com.$name",
        label = name,
        versionName = "1.0",
        versionCode = 1,
        system = false,
        enabled = true,
        installer = null,
        installedAt = 0,
        updatedAt = updated,
        targetSdk = 35,
        minSdk = 26,
        apkBytes = size,
        permissions = permissions.map { (p, granted) -> RequestedPermission("android.permission.$p", PermissionArea.of("android.permission.$p"), granted) },
        special = special,
        launchable = true
    )

    @Test
    fun `permissions map to the areas people know`() {
        assertEquals(PermissionArea.LOCATION, PermissionArea.of("android.permission.ACCESS_COARSE_LOCATION"))
        assertEquals(PermissionArea.PHOTOS_VIDEOS, PermissionArea.of("android.permission.READ_MEDIA_IMAGES"))
        assertNull(PermissionArea.of("android.permission.INTERNET"))
    }

    @Test
    fun `risk comes from what was allowed, not what was asked`() {
        assertEquals(Risk.LOW, app("a", "READ_SMS" to false, "INTERNET" to true).risk)
        assertEquals(Risk.HIGH, app("b", "READ_SMS" to true).risk)
        assertEquals(Risk.MEDIUM, app("c", "CAMERA" to true, "POST_NOTIFICATIONS" to true).risk)
        assertEquals(Risk.HIGH, app("d", special = setOf(SpecialAccess.ACCESSIBILITY)).risk)
        assertEquals(Risk.LOW, app("e", special = setOf(SpecialAccess.EXACT_ALARMS)).risk)
    }

    @Test
    fun `counts per area and risk`() {
        val apps = listOf(
            app("a", "ACCESS_FINE_LOCATION" to true, "ACCESS_COARSE_LOCATION" to true),
            app("b", "ACCESS_FINE_LOCATION" to false),
            app("c", "CAMERA" to true)
        )
        val location = areaCounts(apps).first { it.area == PermissionArea.LOCATION }
        assertEquals(2, location.requested)
        assertEquals(1, location.allowed)
        assertEquals(mapOf(Risk.LOW to 1, Risk.MEDIUM to 2, Risk.HIGH to 0), riskCounts(apps))
    }

    @Test
    fun `sorting and search`() {
        val apps = listOf(app("beta", updated = 5, size = 10), app("Alpha", updated = 9, size = 1))
        assertEquals(listOf("Alpha", "beta"), sortApps(apps, AppSort.NAME).map { it.label })
        assertEquals(listOf("Alpha", "beta"), sortApps(apps, AppSort.UPDATED).map { it.label })
        assertEquals(listOf("beta", "Alpha"), sortApps(apps, AppSort.SIZE).map { it.label })
        assertTrue(apps[0].matches("com.be"))
        assertTrue(apps[1].matches(" alp "))
    }
}
