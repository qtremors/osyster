package dev.qtremors.osyster.core.device.data.pages

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.data.format.DeviceMath
import dev.qtremors.osyster.core.device.data.format.UnitFormat
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection
import dev.qtremors.osyster.core.device.domain.PermissionKind
import java.net.Inet4Address

/** Download and upload rates between two ticks, in bytes per second. */
internal data class LinkSpeed(val downBytesPerSecond: Long, val upBytesPerSecond: Long)

/**
 * The Network page. The Wi-Fi name needs location access and location turned on; mobile network
 * type needs phone access. Without them those rows say so and offer to ask. MAC addresses,
 * BSSIDs, phone numbers, and SIM serials are never read.
 */
internal class NetworkPageCollector(private val context: Context, private val text: DeviceText) {
    private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun collect(speed: LinkSpeed?): List<InfoSection> = listOfNotNull(connection(speed), wifi(), mobile())

    private fun connection(speed: LinkSpeed?) = section(text, "connection", R.string.section_connection, "ConnectivityManager") {
        val network = connectivity?.activeNetwork
        val caps = network?.let { connectivity.getNetworkCapabilities(it) }
        val link = network?.let { connectivity.getLinkProperties(it) }
        row("type", R.string.row_connection_type, caps?.let { text[transport(it)] } ?: text[R.string.value_offline_network], copyable = false)
        if (caps != null) {
            yesNo("internet", R.string.row_internet_validated, caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            yesNo("metered", R.string.row_metered, !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
            yesNo("vpn", R.string.row_vpn, caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
        }
        row("download", R.string.row_download_speed, speed?.let { available(UnitFormat.bytes(it.downBytesPerSecond) + "/s", "TrafficStats") }
            ?: Availability.Unavailable("Measuring", retryable = true), copyable = false)
        row("upload", R.string.row_upload_speed, speed?.let { available(UnitFormat.bytes(it.upBytesPerSecond) + "/s", "TrafficStats") }
            ?: Availability.Unavailable("Measuring", retryable = true), copyable = false)
        val addresses = link?.linkAddresses.orEmpty().map { it.address }
        row("ipv4", R.string.row_local_ipv4, addresses.filterIsInstance<Inet4Address>().joinToString(", ") { it.hostAddress.orEmpty() }.ifBlank { null })
        row("ipv6", R.string.row_local_ipv6, addresses.filterNot { it is Inet4Address }.joinToString("\n") { it.hostAddress.orEmpty() }.ifBlank { null }, advanced = true)
        row("dns", R.string.row_dns, link?.dnsServers?.joinToString(", ") { it.hostAddress.orEmpty() }?.ifBlank { null }, advanced = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && link != null) {
            yesNo("private_dns", R.string.row_private_dns, link.isPrivateDnsActive, advanced = true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && link != null && link.mtu > 0) {
            row("mtu", R.string.row_mtu, link.mtu.toString(), advanced = true, copyable = false)
        }
        if (caps != null) {
            row("bandwidth", R.string.row_estimated_bandwidth, text.format(R.string.value_bandwidth, caps.linkDownstreamBandwidthKbps / 1000, caps.linkUpstreamBandwidthKbps / 1000), advanced = true, copyable = false)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun wifi(): InfoSection? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val caps = connectivity?.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
        return section(text, "wifi", R.string.section_wifi, "WifiManager") {
            yesNo("enabled", R.string.row_wifi_enabled, wifi.isWifiEnabled)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
                row("state", R.string.row_wifi_connection, text[R.string.value_not_connected], copyable = false)
                return@section
            }
            val info = wifi.connectionInfo
            val ssid = info?.ssid?.removeSurrounding("\"")
            row("ssid", R.string.row_wifi_name, when {
                !granted(Manifest.permission.ACCESS_FINE_LOCATION) -> needs(PermissionKind.LOCATION)
                !locationEnabled() -> Availability.Unavailable("Turn on location to see the network name", retryable = true)
                ssid.isNullOrBlank() || ssid == WifiManager.UNKNOWN_SSID -> Availability.Restricted("Android hid the network name")
                else -> available(ssid)
            })
            val frequency = info?.frequency ?: 0
            row("band", R.string.row_wifi_band, if (frequency > 0) text.format(R.string.value_band, DeviceMath.wifiBand(frequency), frequency) else null, copyable = false)
            row("link_speed", R.string.row_link_speed, info?.linkSpeed?.takeIf { it > 0 }?.let { "$it Mbps" }, copyable = false)
            row("signal", R.string.row_signal, info?.rssi?.takeIf { it > -127 }?.let { "$it dBm" }, copyable = false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                row("standard", R.string.row_wifi_standard, DeviceMath.wifiStandard(info?.wifiStandard ?: 0), copyable = false)
                row("max_speed", R.string.row_max_link_speed, info?.maxSupportedTxLinkSpeedMbps?.takeIf { it > 0 }?.let { "$it Mbps" }, advanced = true, copyable = false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                row("security", R.string.row_wifi_security, securityType(info?.currentSecurityType ?: -1), advanced = true, copyable = false)
            }
            yesNo("5ghz", R.string.row_supports_5ghz, wifi.is5GHzBandSupported, advanced = true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) yesNo("6ghz", R.string.row_supports_6ghz, wifi.is6GHzBandSupported, advanced = true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun mobile(): InfoSection? {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return null
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return null
        return section(text, "mobile", R.string.section_mobile, "TelephonyManager") {
            row("carrier", R.string.row_carrier, telephony.networkOperatorName?.ifBlank { null })
            row("sim", R.string.row_sim_state, text[simState(telephony.simState)], copyable = false)
            row("network_type", R.string.row_network_type, if (granted(Manifest.permission.READ_PHONE_STATE)) {
                runCatching { telephony.dataNetworkType }.getOrNull()?.let { available(networkType(it)) }
                    ?: Availability.Unavailable("Not reported", retryable = true)
            } else {
                needs(PermissionKind.PHONE_STATE)
            }, copyable = false)
            yesNo("roaming", R.string.row_roaming, telephony.isNetworkRoaming)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                row("signal_level", R.string.row_signal_level, telephony.signalStrength?.level?.let { text.format(R.string.value_signal_bars, it) }, copyable = false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                row("slots", R.string.row_sim_slots, telephony.activeModemCount.toString(), advanced = true, copyable = false)
            }
            row("country", R.string.row_network_country, telephony.networkCountryIso?.uppercase()?.ifBlank { null }, advanced = true)
        }
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun locationEnabled(): Boolean = (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
        ?.let(LocationManagerCompat::isLocationEnabled) ?: false

    private fun transport(caps: NetworkCapabilities): Int = when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> R.string.transport_wifi
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> R.string.transport_mobile
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> R.string.transport_ethernet
        caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> R.string.transport_bluetooth
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> R.string.transport_vpn
        else -> R.string.value_unknown
    }

    private fun securityType(type: Int): String? = when (type) {
        0 -> text[R.string.value_wifi_open]
        1 -> "WEP"
        2 -> "WPA/WPA2"
        3 -> "Enterprise"
        4 -> "WPA3"
        5 -> "OWE"
        6, 7 -> "WAPI"
        9 -> "Enterprise 192-bit"
        else -> null
    }

    private fun simState(state: Int): Int = when (state) {
        TelephonyManager.SIM_STATE_READY -> R.string.sim_ready
        TelephonyManager.SIM_STATE_ABSENT -> R.string.sim_absent
        TelephonyManager.SIM_STATE_PIN_REQUIRED, TelephonyManager.SIM_STATE_PUK_REQUIRED -> R.string.sim_locked
        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> R.string.sim_network_locked
        else -> R.string.value_unknown
    }

    private fun networkType(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G (NR)"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "Wi-Fi calling (IWLAN)"
        TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
        TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> text[R.string.value_no_data_network]
        else -> "Type $type"
    }
}
