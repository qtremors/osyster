package dev.qtremors.osyster.core.device.data.pages

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection

/**
 * The Bluetooth page. Android has no public "Bluetooth version"; the page lists the LE features
 * the adapter reports and names the lowest version that has all of them, labeled as such. The
 * adapter name and address are never read.
 */
internal class BluetoothPageCollector(private val context: Context, private val text: DeviceText) {

    @SuppressLint("MissingPermission")
    fun collect(): List<InfoSection> {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            return listOf(section(text, "adapter", R.string.section_bluetooth_adapter, "BluetoothManager") {
                row("present", R.string.row_bluetooth_present, Availability.Unsupported("No Bluetooth hardware"))
            })
        }
        val le2m = runCatching { adapter.isLe2MPhySupported }.getOrDefault(false)
        val coded = runCatching { adapter.isLeCodedPhySupported }.getOrDefault(false)
        val extended = runCatching { adapter.isLeExtendedAdvertisingSupported }.getOrDefault(false)
        val periodic = runCatching { adapter.isLePeriodicAdvertisingSupported }.getOrDefault(false)
        val leAudio = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            runCatching { adapter.isLeAudioSupported == BluetoothStatusCodes.FEATURE_SUPPORTED }.getOrDefault(false)
        val leAudioBroadcast = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            runCatching { adapter.isLeAudioBroadcastSourceSupported == BluetoothStatusCodes.FEATURE_SUPPORTED }.getOrDefault(false)

        val adapterSection = section(text, "adapter", R.string.section_bluetooth_adapter, "BluetoothAdapter") {
            yesNo("present", R.string.row_bluetooth_present, true)
            yesNo("enabled", R.string.row_bluetooth_enabled, runCatching { adapter.isEnabled }.getOrDefault(false))
            row("version", R.string.row_bluetooth_version, text[when {
                leAudio -> R.string.bt_version_52
                le2m || coded || extended || periodic -> R.string.bt_version_50
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) -> R.string.bt_version_40
                else -> R.string.bt_version_classic
            }], copyable = false)
        }
        val leSection = section(text, "le", R.string.section_bluetooth_le, "BluetoothAdapter") {
            yesNo("le", R.string.row_bluetooth_le, context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
            yesNo("2m_phy", R.string.row_le_2m_phy, le2m)
            yesNo("coded_phy", R.string.row_le_coded_phy, coded)
            yesNo("extended_adv", R.string.row_le_extended_advertising, extended)
            yesNo("periodic_adv", R.string.row_le_periodic_advertising, periodic)
            yesNo("multi_adv", R.string.row_le_multiple_advertising, runCatching { adapter.isMultipleAdvertisementSupported }.getOrDefault(false), advanced = true)
            yesNo("offload_filter", R.string.row_le_offloaded_filtering, runCatching { adapter.isOffloadedFilteringSupported }.getOrDefault(false), advanced = true)
        }
        val audioSection = section(text, "audio", R.string.section_bluetooth_audio, "BluetoothAdapter") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                yesNo("le_audio", R.string.row_le_audio, leAudio)
                yesNo("le_broadcast", R.string.row_le_audio_broadcast, leAudioBroadcast)
            } else {
                row("le_audio", R.string.row_le_audio, Availability.Unsupported("Needs Android 13"))
            }
        }
        return listOf(adapterSection, leSection, audioSection)
    }
}
