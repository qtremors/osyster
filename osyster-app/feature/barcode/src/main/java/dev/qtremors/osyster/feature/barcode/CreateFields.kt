package dev.qtremors.osyster.feature.barcode

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.barcode.domain.CodeField
import dev.qtremors.osyster.core.barcode.domain.FieldType
import dev.qtremors.osyster.core.barcode.domain.TRUE
import dev.qtremors.osyster.core.barcode.domain.WifiSecurity
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ToggleRow
import dev.qtremors.osyster.core.ui.theme.FieldShape
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * One form field. [error] shows under it when the code cannot be made because of this field.
 * [values] is the whole form, for fields that depend on another (an all-day event hides times).
 */
@Composable
internal fun CreateField(
    field: CodeField,
    value: String,
    values: Map<CodeField, String>,
    error: String?,
    onChange: (CodeField, String) -> Unit
) {
    when (field.type) {
        FieldType.SWITCH -> ConnectedGroup {
            ToggleRow(title = stringResource(field.label), checked = value == TRUE, onCheckedChange = { onChange(field, if (it) TRUE else "") })
        }
        FieldType.SECURITY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(field.label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ConnectedChoices(
                options = WifiSecurity.entries,
                selected = WifiSecurity.entries.firstOrNull { it.name == value } ?: WifiSecurity.WPA,
                label = { stringResource(it.label) },
                onSelect = { onChange(field, it.name) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        FieldType.DATE_TIME -> DateTimeField(field, value.toLongOrNull(), allDay = values[CodeField.ALL_DAY] == TRUE, error = error) { onChange(field, "$it") }
        else -> TextEntry(field, value, error, onChange)
    }
}

@Composable
private fun TextEntry(field: CodeField, value: String, error: String?, onChange: (CodeField, String) -> Unit) {
    var shown by rememberSaveable { mutableStateOf(false) }
    val type = field.type
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(field, it) },
        shape = FieldShape,
        label = { Text(stringResource(field.label)) },
        placeholder = if (type == FieldType.PACKAGE) ({ Text(stringResource(R.string.create_field_package_hint)) }) else null,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = type != FieldType.MULTILINE,
        minLines = if (type == FieldType.MULTILINE) 2 else 1,
        maxLines = if (type == FieldType.MULTILINE) 6 else 1,
        visualTransformation = if (type == FieldType.PASSWORD && !shown) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (type == FieldType.PASSWORD) ({
            IconButton(shapes = IconButtonDefaults.shapes(), onClick = { shown = !shown }) {
                Icon(
                    if (shown) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(if (shown) R.string.create_field_password_hide else R.string.create_field_password_show)
                )
            }
        }) else null,
        keyboardOptions = keyboardFor(type),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun keyboardFor(type: FieldType): KeyboardOptions = when (type) {
    FieldType.URL -> KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Next)
    FieldType.PHONE -> KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
    FieldType.EMAIL -> KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Next)
    FieldType.PASSWORD -> KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Next)
    FieldType.DIGITS -> KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
    FieldType.DECIMAL -> KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
    FieldType.PACKAGE -> KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done)
    FieldType.MULTILINE -> KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    else -> KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
}

/** A date and a time, each on its own tonal button; the time hides for all-day events. */
@Composable
private fun DateTimeField(field: CodeField, millis: Long?, allDay: Boolean, error: String?, onPick: (Long) -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val moment = (millis ?: System.currentTimeMillis()).let { Instant.ofEpochMilli(it).atZone(zone) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(field.label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { picking = PICK_DATE }, modifier = Modifier.weight(1f).height(56.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                Text(DateFormat.getMediumDateFormat(context).format(java.util.Date(moment.toInstant().toEpochMilli())), modifier = Modifier.padding(start = 8.dp))
            }
            if (!allDay) {
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { picking = PICK_TIME }, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null)
                    Text(DateFormat.getTimeFormat(context).format(java.util.Date(moment.toInstant().toEpochMilli())), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp)) }
    }
    when (picking) {
        PICK_DATE -> {
            // The date picker works in UTC days; the picked day keeps the time already chosen.
            val state = rememberDatePickerState(initialSelectedDateMillis = moment.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            DatePickerDialog(
                onDismissRequest = { picking = null },
                confirmButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                        state.selectedDateMillis?.let { picked ->
                            val day = Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate()
                            onPick(day.atTime(moment.toLocalTime()).atZone(zone).toInstant().toEpochMilli())
                        }
                        picking = null
                    }) { Text(stringResource(R.string.create_ok)) }
                },
                dismissButton = { TextButton(shapes = ButtonDefaults.shapes(), onClick = { picking = null }) { Text(stringResource(android.R.string.cancel)) } }
            ) { DatePicker(state = state) }
        }
        PICK_TIME -> {
            val state = rememberTimePickerState(initialHour = moment.hour, initialMinute = moment.minute, is24Hour = DateFormat.is24HourFormat(context))
            AlertDialog(
                onDismissRequest = { picking = null },
                title = { Text(stringResource(R.string.create_pick_time)) },
                text = { TimePicker(state = state) },
                confirmButton = {
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                        val day: LocalDate = moment.toLocalDate()
                        onPick(day.atTime(LocalTime.of(state.hour, state.minute)).atZone(zone).toInstant().toEpochMilli())
                        picking = null
                    }) { Text(stringResource(R.string.create_ok)) }
                },
                dismissButton = { TextButton(shapes = ButtonDefaults.shapes(), onClick = { picking = null }) { Text(stringResource(android.R.string.cancel)) } }
            )
        }
    }
}

private const val PICK_DATE = "date"
private const val PICK_TIME = "time"
