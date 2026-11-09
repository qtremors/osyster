package dev.qtremors.osyster.core.device.domain

import kotlinx.coroutines.flow.Flow

/**
 * Supplies the sections of each device page. Static pages emit once; live pages (processor,
 * memory, battery) keep emitting while collected and stop when the page closes.
 */
interface DevicePageSource {
    /** False for pages whose collector has not shipped yet; the UI shows "On its way". */
    fun isBuilt(page: DevicePage): Boolean

    fun sections(page: DevicePage): Flow<List<InfoSection>>
}
