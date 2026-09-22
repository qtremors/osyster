package dev.qtremors.osyster.core.device.domain

/**
 * A plain-text summary of every device page, for the user to share or keep. It contains no
 * serial numbers, IMEI, MAC addresses, or network names.
 */
interface DeviceReportSource {
    suspend fun report(): String
}
