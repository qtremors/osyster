package dev.qtremors.osyster.feature.barcode

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.core.barcode.domain.CodeField
import dev.qtremors.osyster.core.barcode.domain.CreateKind
import dev.qtremors.osyster.core.barcode.domain.CreateProblem
import dev.qtremors.osyster.core.barcode.domain.Recovery
import dev.qtremors.osyster.core.barcode.domain.StyleWarning
import dev.qtremors.osyster.core.barcode.domain.WifiSecurity

/** A kind's name on screen: QR kinds by what they hold, the others by their format's name. */
@Composable
internal fun CreateKind.title(): String = when (this) {
    CreateKind.TEXT -> stringResource(R.string.create_kind_text)
    CreateKind.LINK -> stringResource(R.string.create_kind_link)
    CreateKind.WIFI -> stringResource(R.string.create_kind_wifi)
    CreateKind.CONTACT -> stringResource(R.string.create_kind_contact)
    CreateKind.EVENT -> stringResource(R.string.create_kind_event)
    CreateKind.SMS -> stringResource(R.string.create_kind_sms)
    CreateKind.EMAIL -> stringResource(R.string.create_kind_email)
    CreateKind.PHONE -> stringResource(R.string.create_kind_phone)
    CreateKind.LOCATION -> stringResource(R.string.create_kind_location)
    CreateKind.APP -> stringResource(R.string.create_kind_app)
    else -> format.label
}

internal val CreateKind.detail: Int
    get() = when (this) {
        CreateKind.TEXT -> R.string.create_kind_text_detail
        CreateKind.LINK -> R.string.create_kind_link_detail
        CreateKind.WIFI -> R.string.create_kind_wifi_detail
        CreateKind.CONTACT -> R.string.create_kind_contact_detail
        CreateKind.EVENT -> R.string.create_kind_event_detail
        CreateKind.SMS -> R.string.create_kind_sms_detail
        CreateKind.EMAIL -> R.string.create_kind_email_detail
        CreateKind.PHONE -> R.string.create_kind_phone_detail
        CreateKind.LOCATION -> R.string.create_kind_location_detail
        CreateKind.APP -> R.string.create_kind_app_detail
        CreateKind.DATA_MATRIX -> R.string.create_kind_matrix_detail
        CreateKind.AZTEC -> R.string.create_kind_aztec_detail
        CreateKind.PDF_417 -> R.string.create_kind_pdf417_detail
        CreateKind.EAN_13 -> R.string.create_kind_ean13_detail
        CreateKind.EAN_8 -> R.string.create_kind_ean8_detail
        CreateKind.UPC_A -> R.string.create_kind_upca_detail
        CreateKind.UPC_E -> R.string.create_kind_upce_detail
        CreateKind.CODE_128 -> R.string.create_kind_code128_detail
        CreateKind.CODE_93 -> R.string.create_kind_code93_detail
        CreateKind.CODE_39 -> R.string.create_kind_code39_detail
        CreateKind.CODABAR -> R.string.create_kind_codabar_detail
        CreateKind.ITF -> R.string.create_kind_itf_detail
    }

internal val CreateKind.icon: ImageVector
    get() = when (this) {
        CreateKind.TEXT -> Icons.AutoMirrored.Rounded.Notes
        CreateKind.LINK -> Icons.Rounded.Link
        CreateKind.WIFI -> Icons.Rounded.Wifi
        CreateKind.CONTACT -> Icons.Rounded.Contacts
        CreateKind.EVENT -> Icons.Rounded.Event
        CreateKind.SMS -> Icons.Rounded.Sms
        CreateKind.EMAIL -> Icons.Rounded.Email
        CreateKind.PHONE -> Icons.Rounded.Phone
        CreateKind.LOCATION -> Icons.Rounded.Place
        CreateKind.APP -> Icons.Rounded.Apps
        else -> if (format.twoD) Icons.Rounded.QrCode2 else Icons.Rounded.ViewWeek
    }

internal val CodeField.label: Int
    get() = when (this) {
        CodeField.TEXT -> R.string.create_field_text
        CodeField.URL -> R.string.create_field_url
        CodeField.SSID -> R.string.create_field_ssid
        CodeField.PASSWORD -> R.string.create_field_password
        CodeField.SECURITY -> R.string.create_field_security
        CodeField.HIDDEN -> R.string.create_field_hidden
        CodeField.NAME -> R.string.create_field_name
        CodeField.ORGANIZATION -> R.string.create_field_organization
        CodeField.JOB_TITLE -> R.string.create_field_job_title
        CodeField.PHONE -> R.string.create_field_phone
        CodeField.EMAIL -> R.string.create_field_email
        CodeField.ADDRESS -> R.string.create_field_address
        CodeField.WEBSITE -> R.string.create_field_website
        CodeField.NOTE -> R.string.create_field_note
        CodeField.SUMMARY -> R.string.create_field_summary
        CodeField.START -> R.string.create_field_start
        CodeField.END -> R.string.create_field_end
        CodeField.ALL_DAY -> R.string.create_field_all_day
        CodeField.PLACE -> R.string.create_field_place
        CodeField.SUBJECT -> R.string.create_field_subject
        CodeField.MESSAGE -> R.string.create_field_message
        CodeField.LATITUDE -> R.string.create_field_latitude
        CodeField.LONGITUDE -> R.string.create_field_longitude
        CodeField.QUERY -> R.string.create_field_query
        CodeField.PACKAGE -> R.string.create_field_package
        CodeField.DIGITS -> R.string.create_field_digits
        CodeField.CONTENT -> R.string.create_field_content
    }

internal val WifiSecurity.label: Int
    get() = when (this) {
        WifiSecurity.WPA -> R.string.create_security_wpa
        WifiSecurity.WEP -> R.string.create_security_wep
        WifiSecurity.NONE -> R.string.create_security_none
    }

internal val Recovery.label: Int
    get() = when (this) {
        Recovery.LOW -> R.string.create_recovery_low
        Recovery.MEDIUM -> R.string.create_recovery_medium
        Recovery.QUARTILE -> R.string.create_recovery_quartile
        Recovery.HIGH -> R.string.create_recovery_high
    }

internal val ExportSize.label: Int
    get() = when (this) {
        ExportSize.SMALL -> R.string.create_size_small
        ExportSize.MEDIUM -> R.string.create_size_medium
        ExportSize.LARGE -> R.string.create_size_large
    }

internal val StyleWarning.message: Int
    get() = when (this) {
        StyleWarning.LOW_CONTRAST -> R.string.create_warning_contrast
        StyleWarning.INVERTED -> R.string.create_warning_inverted
    }

/** What to tell the person about [problem]. A missing field is a nudge, not an error. */
@Composable
internal fun CreateProblem.message(): String = when (this) {
    is CreateProblem.Missing -> stringResource(R.string.create_problem_missing, stringResource(field.label).lowercase())
    is CreateProblem.DigitCount -> stringResource(R.string.create_problem_digits, counts[0], counts[1])
    is CreateProblem.Checksum -> stringResource(R.string.create_problem_checksum)
    is CreateProblem.UpcESystem -> stringResource(R.string.create_problem_upce)
    is CreateProblem.EvenDigits -> stringResource(R.string.create_problem_even)
    is CreateProblem.Characters -> stringResource(R.string.create_problem_characters, allowed)
    is CreateProblem.Range -> stringResource(R.string.create_problem_range, min.toInt().toString(), max.toInt().toString())
    is CreateProblem.PackageName -> stringResource(R.string.create_problem_package)
    is CreateProblem.EndBeforeStart -> stringResource(R.string.create_problem_end)
    CreateProblem.TooLong -> stringResource(R.string.create_problem_long)
}
