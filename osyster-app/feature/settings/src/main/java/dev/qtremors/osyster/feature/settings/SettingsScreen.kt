package dev.qtremors.osyster.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.ChoiceRow
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.NavigationRow
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics
import dev.qtremors.osyster.core.ui.theme.AccentSource
import dev.qtremors.osyster.core.ui.theme.ThemeMode
import dev.qtremors.osyster.core.ui.theme.ThemeOptions
import dev.qtremors.osyster.core.ui.theme.ThemePreset
import dev.qtremors.osyster.core.ui.theme.seedColorScheme

@Immutable
internal data class SettingsActions(
    val onMode: (ThemeMode) -> Unit,
    val onOled: (Boolean) -> Unit,
    val onAccentSource: (AccentSource) -> Unit,
    val onPreset: (ThemePreset) -> Unit,
    val onHaptics: (Boolean) -> Unit,
    val onReduceMotion: (Boolean) -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onShowWelcome: () -> Unit,
    val onMessageShown: () -> Unit,
    val onNavigate: (SettingsDestination) -> Unit
)

@Composable
internal fun SettingsScreen(
    theme: ThemeOptions?,
    message: SettingsMessage?,
    actions: SettingsActions
) {
    val snackbar = remember { SnackbarHostState() }
    val messageText = message?.let { stringResource(it.textRes()) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            actions.onMessageShown()
        }
    }
    OsysterScaffold(title = stringResource(R.string.settings_title), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "appearance") {
                if (theme == null) LoadingState() else AppearanceSection(theme, actions)
            }
            item(key = "feel") {
                if (theme != null) FeelSection(theme, actions)
            }
            item(key = "backup") { BackupSection(actions) }
            item(key = "about") { AboutSection(actions.onNavigate) }
        }
    }
}

@Composable
private fun AppearanceSection(theme: ThemeOptions, actions: SettingsActions) {
    val wallpaperAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Column {
        SectionHeader(stringResource(R.string.settings_appearance))
        ConnectedGroup {
            ChoiceRow(
                title = stringResource(R.string.settings_theme),
                icon = Icons.Rounded.DarkMode,
                options = ThemeMode.entries,
                selected = theme.mode,
                label = { mode -> stringResource(mode.labelRes()) },
                onSelect = actions.onMode
            )
            ToggleRow(
                title = stringResource(R.string.settings_oled),
                supporting = stringResource(R.string.settings_oled_summary),
                icon = Icons.Rounded.Contrast,
                checked = theme.oled,
                onCheckedChange = actions.onOled,
                enabled = theme.mode != ThemeMode.LIGHT
            )
            if (wallpaperAvailable) {
                ChoiceRow(
                    title = stringResource(R.string.settings_colors),
                    icon = Icons.Rounded.Palette,
                    options = listOf(AccentSource.WALLPAPER, AccentSource.PRESET),
                    selected = if (theme.accentSource == AccentSource.WALLPAPER) AccentSource.WALLPAPER else AccentSource.PRESET,
                    label = { source -> stringResource(if (source == AccentSource.WALLPAPER) R.string.settings_colors_wallpaper else R.string.settings_colors_preset) },
                    onSelect = actions.onAccentSource
                )
            }
            if (!wallpaperAvailable || theme.accentSource != AccentSource.WALLPAPER) {
                PresetPicker(selected = theme.preset, onSelect = actions.onPreset)
            }
        }
    }
}

@Composable
private fun FeelSection(theme: ThemeOptions, actions: SettingsActions) {
    Column {
        SectionHeader(stringResource(R.string.settings_feel))
        ConnectedGroup {
            ToggleRow(
                title = stringResource(R.string.settings_haptics),
                supporting = stringResource(R.string.settings_haptics_summary),
                icon = Icons.Rounded.Vibration,
                checked = theme.hapticsEnabled,
                onCheckedChange = actions.onHaptics
            )
            ToggleRow(
                title = stringResource(R.string.settings_reduce_motion),
                supporting = stringResource(R.string.settings_reduce_motion_summary),
                icon = Icons.Rounded.Animation,
                checked = theme.reduceMotion,
                onCheckedChange = actions.onReduceMotion
            )
        }
    }
}

/** Seed swatches; each shows the primary color its scheme would produce, not the raw seed. */
@Composable
private fun PresetPicker(selected: ThemePreset, onSelect: (ThemePreset) -> Unit) {
    val haptics = rememberAppHaptics()
    val dark = MaterialTheme.colorScheme.surface.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    Surface(color = MaterialTheme.colorScheme.surfaceBright, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemePreset.entries.forEach { preset ->
                val swatch = seedColorScheme(preset.seed, dark).primary
                val isSelected = preset == selected
                val name = stringResource(preset.labelRes())
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(swatch, CircleShape)
                        .border(if (isSelected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .selectable(selected = isSelected, role = Role.RadioButton) {
                            haptics.tick()
                            onSelect(preset)
                        }
                        .semantics { contentDescription = name },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupSection(actions: SettingsActions) {
    Column {
        SectionHeader(stringResource(R.string.settings_backup))
        ConnectedGroup {
            NavigationRow(stringResource(R.string.settings_export), Icons.Rounded.Upload, "export", actions.onExport, supporting = stringResource(R.string.settings_export_summary))
            NavigationRow(stringResource(R.string.settings_import), Icons.Rounded.Download, "import", actions.onImport, supporting = stringResource(R.string.settings_import_summary))
            NavigationRow(stringResource(R.string.settings_show_welcome), Icons.Rounded.WavingHand, "welcome", actions.onShowWelcome, supporting = stringResource(R.string.settings_show_welcome_summary))
        }
    }
}

@Composable
private fun AboutSection(onNavigate: (SettingsDestination) -> Unit) {
    Column {
        SectionHeader(stringResource(R.string.settings_about))
        ConnectedGroup {
            NavigationRow(stringResource(R.string.about_title), Icons.Rounded.Info, "about", { onNavigate(SettingsDestination.About) }, supporting = stringResource(R.string.about_summary))
            NavigationRow(stringResource(R.string.about_privacy), Icons.Rounded.PrivacyTip, "privacy", { onNavigate(SettingsDestination.Privacy) }, supporting = stringResource(R.string.about_privacy_summary))
            NavigationRow(stringResource(R.string.about_licenses), Icons.Rounded.Favorite, "licenses", { onNavigate(SettingsDestination.Licenses) }, supporting = stringResource(R.string.about_licenses_summary))
        }
    }
}

private fun SettingsMessage.textRes(): Int = when (this) {
    SettingsMessage.EXPORTED -> R.string.settings_exported
    SettingsMessage.EXPORT_FAILED -> R.string.settings_export_failed
    SettingsMessage.IMPORTED -> R.string.settings_imported
    SettingsMessage.IMPORT_FAILED -> R.string.settings_import_failed
    SettingsMessage.NOT_A_BACKUP -> R.string.settings_not_a_backup
    SettingsMessage.WELCOME_RESET -> R.string.settings_welcome_reset
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun ThemePreset.labelRes(): Int = when (this) {
    ThemePreset.CYAN -> R.string.preset_cyan
    ThemePreset.AMBER -> R.string.preset_amber
    ThemePreset.CORAL -> R.string.preset_coral
    ThemePreset.VIOLET -> R.string.preset_violet
    ThemePreset.MINT -> R.string.preset_mint
    ThemePreset.SLATE -> R.string.preset_slate
}
