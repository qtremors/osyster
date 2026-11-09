package dev.qtremors.osyster.core.device.data.pages

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.data.read.CpuReader
import dev.qtremors.osyster.core.device.domain.InfoSection

/**
 * The Device page: who made the phone, what it runs on, and which hardware features it declares.
 * Never shows serial numbers, IMEI, or other persistent identifiers.
 */
internal class DeviceInfoCollector(private val context: Context, private val text: DeviceText) {

    fun collect(): List<InfoSection> = listOf(identity(), platform(), features())

    private fun identity() = section(text, "identity", R.string.section_identity, "Build") {
        row("model", R.string.row_model, Build.MODEL)
        row("manufacturer", R.string.row_manufacturer, Build.MANUFACTURER.replaceFirstChar { it.titlecase() })
        row("brand", R.string.row_brand, Build.BRAND.replaceFirstChar { it.titlecase() })
        row("codename", R.string.row_codename, Build.DEVICE)
        row("device_type", R.string.row_device_type, text[deviceType()], copyable = false, source = "PackageManager")
        row("product", R.string.row_product, Build.PRODUCT, advanced = true)
    }

    private fun platform() = section(text, "platform", R.string.section_platform, "Build") {
        row("chip", R.string.row_chip, CpuReader.chipName())
        row("hardware", R.string.row_hardware, Build.HARDWARE)
        row("board", R.string.row_board, Build.BOARD)
        row("abis", R.string.row_supported_abis, Build.SUPPORTED_ABIS.joinToString(", "))
        yesNo("is_64_bit", R.string.row_64_bit, Build.SUPPORTED_64_BIT_ABIS.isNotEmpty())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            row("sku", R.string.row_sku, Build.SKU, advanced = true)
            row("odm_sku", R.string.row_odm_sku, Build.ODM_SKU, advanced = true)
        }
    }

    private fun features() = section(text, "features", R.string.section_features, "PackageManager") {
        val pm = context.packageManager
        fun has(feature: String) = pm.hasSystemFeature(feature)
        yesNo("nfc", R.string.feature_nfc, has(PackageManager.FEATURE_NFC))
        yesNo("nfc_payments", R.string.feature_nfc_payments, has(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION))
        yesNo("fingerprint", R.string.feature_fingerprint, has(PackageManager.FEATURE_FINGERPRINT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) yesNo("face", R.string.feature_face, has(PackageManager.FEATURE_FACE))
        yesNo("ir", R.string.feature_ir, has(PackageManager.FEATURE_CONSUMER_IR))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) yesNo("uwb", R.string.feature_uwb, has(PackageManager.FEATURE_UWB))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) yesNo("esim", R.string.feature_esim, has(PackageManager.FEATURE_TELEPHONY_EUICC))
        yesNo("telephony", R.string.feature_telephony, has(PackageManager.FEATURE_TELEPHONY))
        yesNo("wifi_aware", R.string.feature_wifi_aware, has(PackageManager.FEATURE_WIFI_AWARE))
        yesNo("wifi_direct", R.string.feature_wifi_direct, has(PackageManager.FEATURE_WIFI_DIRECT))
        yesNo("bluetooth_le", R.string.feature_bluetooth_le, has(PackageManager.FEATURE_BLUETOOTH_LE))
        yesNo("usb_host", R.string.feature_usb_host, has(PackageManager.FEATURE_USB_HOST))
        yesNo("vulkan", R.string.feature_vulkan, has(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) yesNo("hinge", R.string.feature_hinge, has(PackageManager.FEATURE_SENSOR_HINGE_ANGLE), advanced = true)
        yesNo("usb_accessory", R.string.feature_usb_accessory, has(PackageManager.FEATURE_USB_ACCESSORY), advanced = true)
        yesNo("midi", R.string.feature_midi, has(PackageManager.FEATURE_MIDI), advanced = true)
        row("feature_count", R.string.row_feature_count, pm.systemAvailableFeatures.count { it.name != null }.toString(), advanced = true, copyable = false)
    }

    private fun deviceType(): Int {
        val uiMode = (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType
        val pm = context.packageManager
        return when {
            uiMode == Configuration.UI_MODE_TYPE_TELEVISION -> R.string.device_type_tv
            uiMode == Configuration.UI_MODE_TYPE_WATCH -> R.string.device_type_watch
            uiMode == Configuration.UI_MODE_TYPE_CAR -> R.string.device_type_car
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE) -> R.string.device_type_foldable
            context.resources.configuration.smallestScreenWidthDp >= 600 -> R.string.device_type_tablet
            else -> R.string.device_type_phone
        }
    }
}
