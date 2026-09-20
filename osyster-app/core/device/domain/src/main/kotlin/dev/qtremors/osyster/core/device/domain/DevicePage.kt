package dev.qtremors.osyster.core.device.domain

/**
 * The device information pages listed on Home, in default order.
 *
 * [id] is persisted (favorites, custom order, hidden rows) and used in routes, so it never
 * changes once released. Titles and icons are UI concerns and live in the Home feature.
 */
enum class DevicePage(val id: String, val group: DevicePageGroup) {
    SYSTEM("system", DevicePageGroup.ABOUT),
    DEVICE("device", DevicePageGroup.ABOUT),
    PROCESSOR("processor", DevicePageGroup.PERFORMANCE),
    MEMORY("memory", DevicePageGroup.PERFORMANCE),
    BATTERY("battery", DevicePageGroup.PERFORMANCE),
    DISPLAY("display", DevicePageGroup.HARDWARE),
    CAMERAS("cameras", DevicePageGroup.HARDWARE),
    SENSORS("sensors", DevicePageGroup.HARDWARE),
    NETWORK("network", DevicePageGroup.CONNECTIVITY),
    BLUETOOTH("bluetooth", DevicePageGroup.CONNECTIVITY),
    MEDIA("media", DevicePageGroup.MEDIA_SECURITY),
    CERTIFICATES("certificates", DevicePageGroup.MEDIA_SECURITY);

    companion object {
        private val byId = entries.associateBy(DevicePage::id)

        fun fromId(id: String): DevicePage? = byId[id]

        /** Restores a saved order: unknown ids are dropped, new pages are appended. */
        fun restoreOrder(savedIds: List<String>): List<DevicePage> {
            val saved = savedIds.mapNotNull(::fromId).distinct()
            return saved + entries.filterNot(saved::contains)
        }
    }
}

enum class DevicePageGroup {
    ABOUT,
    PERFORMANCE,
    HARDWARE,
    CONNECTIVITY,
    MEDIA_SECURITY
}
