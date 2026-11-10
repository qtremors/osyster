package dev.qtremors.osyster.feature.btremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.hid.domain.KeyCodes
import dev.qtremors.osyster.core.hid.domain.KeyStroke
import dev.qtremors.osyster.core.hid.domain.Modifiers
import dev.qtremors.osyster.core.hid.domain.typingChange
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics
import dev.qtremors.osyster.core.ui.theme.FieldShape

/**
 * Typing on the TV or computer. Text goes out as it is typed: added characters are typed, removed
 * ones become Backspace, so any phone keyboard works, swipe typing included. Ctrl, Alt, Shift,
 * and Win stick for the next key, for shortcuts like Ctrl+C. The keys below cover what a phone
 * keyboard lacks; arrows and Backspace repeat while held.
 */
@Composable
internal fun KeyboardContent(viewModel: RemoteViewModel) {
    val modifiers by viewModel.modifiers.collectAsStateWithLifecycle()
    var field by remember { mutableStateOf(TextFieldValue("")) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            shape = FieldShape,
            value = field,
            onValueChange = { next ->
                val change = typingChange(field.text, next.text)
                viewModel.backspace(change.backspaces)
                if (change.added.isNotEmpty()) viewModel.typeText(change.added)
                field = next
            },
            label = { Text(stringResource(R.string.remote_keyboard_field)) },
            supportingText = { Text(stringResource(R.string.remote_keyboard_field_hint)) },
            trailingIcon = {
                if (field.text.isNotEmpty()) {
                    // Clears the field here only; nothing is deleted on the TV or computer.
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { field = TextFieldValue("", TextRange.Zero) }) {
                        Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.remote_keyboard_clear))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { viewModel.pressKey(KeyStroke(KeyCodes.ENTER)) }),
            modifier = Modifier.fillMaxWidth()
        )

        ModifierToggles(modifiers = modifiers, onToggle = viewModel::toggleModifier)

        val hold = { stroke: KeyStroke -> { viewModel.holdKey(stroke) } }
        val release = viewModel::releaseKey
        KeyRow {
            TextKey(stringResource(R.string.remote_key_esc), hold(KeyStroke(KeyCodes.ESCAPE)), release)
            TextKey(stringResource(R.string.remote_key_tab), hold(KeyStroke(KeyCodes.TAB)), release)
            IconKey(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.remote_key_backspace), hold(KeyStroke(KeyCodes.BACKSPACE)), release)
            IconKey(Icons.AutoMirrored.Rounded.KeyboardReturn, stringResource(R.string.remote_digit_enter), hold(KeyStroke(KeyCodes.ENTER)), release)
        }
        KeyRow {
            TextKey(stringResource(R.string.remote_key_home), hold(KeyStroke(KeyCodes.HOME)), release)
            TextKey(stringResource(R.string.remote_key_end), hold(KeyStroke(KeyCodes.END)), release)
            TextKey(stringResource(R.string.remote_key_page_up), hold(KeyStroke(KeyCodes.PAGE_UP)), release)
            TextKey(stringResource(R.string.remote_key_page_down), hold(KeyStroke(KeyCodes.PAGE_DOWN)), release)
        }
        KeyRow {
            TextKey(stringResource(R.string.remote_key_insert), hold(KeyStroke(KeyCodes.INSERT)), release)
            TextKey(stringResource(R.string.remote_key_delete), hold(KeyStroke(KeyCodes.DELETE)), release)
            TextKey(stringResource(R.string.remote_key_print_screen), hold(KeyStroke(KeyCodes.PRINT_SCREEN)), release)
        }
        KeyRow {
            IconKey(Icons.Rounded.KeyboardArrowLeft, stringResource(R.string.remote_left), hold(KeyStroke(KeyCodes.LEFT)), release)
            IconKey(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.remote_up), hold(KeyStroke(KeyCodes.UP)), release)
            IconKey(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.remote_down), hold(KeyStroke(KeyCodes.DOWN)), release)
            IconKey(Icons.Rounded.KeyboardArrowRight, stringResource(R.string.remote_right), hold(KeyStroke(KeyCodes.RIGHT)), release)
        }
        (1..12).chunked(6).forEach { row ->
            KeyRow {
                row.forEach { n -> TextKey(stringResource(R.string.remote_key_function, n), hold(KeyStroke(KeyCodes.function(n))), release) }
            }
        }
    }
}

/** Ctrl, Alt, Shift, and Win as joined toggle buttons; a lit one applies to the next key. */
@Composable
private fun ModifierToggles(modifiers: Int, onToggle: (Int) -> Unit) {
    val haptics = rememberAppHaptics()
    val keys = listOf(
        Modifiers.CTRL to R.string.remote_key_ctrl,
        Modifiers.ALT to R.string.remote_key_alt,
        Modifiers.SHIFT to R.string.remote_key_shift,
        Modifiers.META to R.string.remote_key_win
    )
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween), modifier = Modifier.fillMaxWidth()) {
        keys.forEachIndexed { index, (bit, label) ->
            ToggleButton(
                checked = modifiers and bit != 0,
                onCheckedChange = {
                    haptics.tick()
                    onToggle(bit)
                },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    keys.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text(stringResource(label), maxLines = 1) }
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun RowScope.TextKey(text: String, onPress: () -> Unit, onRelease: () -> Unit) {
    PressKey(text, onPress, onRelease, Modifier.weight(1f).height(48.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RowScope.IconKey(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onPress: () -> Unit, onRelease: () -> Unit) {
    PressKey(icon, label, onPress, onRelease, Modifier.weight(1f).height(48.dp), iconSize = 22.dp)
}
