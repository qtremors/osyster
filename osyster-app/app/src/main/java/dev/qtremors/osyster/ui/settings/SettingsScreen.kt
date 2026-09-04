@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.R
import dev.qtremors.osyster.settings.DiagnosticsInterval
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.settings.OsysterPreferencesState
import dev.qtremors.osyster.settings.TemperatureUnit
import dev.qtremors.osyster.ui.theme.expressiveSegmentedShapes

@Composable
fun SettingsScreen(
    state: OsysterPreferencesState,
    manager: OsysterPreferencesManager,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Appearance Section
            item {
                SettingsSection(title = stringResource(R.string.section_appearance)) {
                    ThemeModeSelector(
                        currentMode = state.themeMode,
                        onModeSelected = manager::setThemeMode
                    )
                }
            }

            item {
                AccentPaletteSelector(
                    currentPalette = state.accentPalette,
                    onPaletteSelected = manager::setAccentPalette
                )
            }

            item {
                val hasDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                val switchCount = if (hasDynamicColorSupport) 2 else 1

                SettingsSection(title = "") {
                    if (hasDynamicColorSupport) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.dynamic_color),
                            description = stringResource(R.string.dynamic_color_description),
                            checked = state.dynamicColor,
                            onCheckedChange = manager::setDynamicColor,
                            index = 0,
                            count = switchCount,
                            leadingIcon = Icons.Default.ColorLens
                        )
                    }
                    SettingsSwitchItem(
                        title = stringResource(R.string.haptic_feedback),
                        description = stringResource(R.string.haptic_feedback_description),
                        checked = state.hapticFeedback,
                        onCheckedChange = manager::setHapticFeedback,
                        index = if (hasDynamicColorSupport) 1 else 0,
                        count = switchCount,
                        leadingIcon = Icons.Default.Vibration
                    )
                }
            }

            // Diagnostics & Telemetry Section
            item {
                SettingsSection(title = stringResource(R.string.section_diagnostics)) {
                    val intervals = listOf(
                        DiagnosticsInterval.INTERVAL_500MS to stringResource(R.string.interval_fast),
                        DiagnosticsInterval.INTERVAL_1000MS to stringResource(R.string.interval_normal),
                        DiagnosticsInterval.INTERVAL_2000MS to stringResource(R.string.interval_relaxed),
                        DiagnosticsInterval.INTERVAL_3000MS to stringResource(R.string.interval_saver)
                    )

                    intervals.forEachIndexed { index, (interval, label) ->
                        val isSelected = state.diagnosticsInterval == interval
                        SegmentedListItem(
                            onClick = { manager.setDiagnosticsInterval(interval) },
                            shapes = expressiveSegmentedShapes(index = index, count = intervals.size),
                            content = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            supportingContent = if (index == 0) {
                                { Text(stringResource(R.string.update_interval_description)) }
                            } else null,
                            leadingContent = if (index == 0) {
                                {
                                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else null,
                            trailingContent = {
                                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                }
                            },
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.height(IntrinsicSize.Min)
                        )
                    }
                }
            }

            // Temperature Units
            item {
                SettingsSection(title = stringResource(R.string.temp_unit)) {
                    val units = listOf(
                        TemperatureUnit.CELSIUS to stringResource(R.string.unit_celsius),
                        TemperatureUnit.FAHRENHEIT to stringResource(R.string.unit_fahrenheit)
                    )

                    units.forEachIndexed { index, (unit, label) ->
                        val isSelected = state.temperatureUnit == unit
                        SegmentedListItem(
                            onClick = { manager.setTemperatureUnit(unit) },
                            shapes = expressiveSegmentedShapes(index = index, count = units.size),
                            content = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingContent = if (index == 0) {
                                {
                                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Thermostat,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else null,
                            trailingContent = {
                                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                }
                            },
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.height(IntrinsicSize.Min)
                        )
                    }
                }
            }

            // Process Filter
            item {
                SettingsSection(title = stringResource(R.string.process_filter)) {
                    SettingsSwitchItem(
                        title = stringResource(R.string.process_filter_title),
                        description = stringResource(R.string.process_filter_description),
                        checked = state.showKernelThreads,
                        onCheckedChange = manager::setShowKernelThreads,
                        index = 0,
                        count = 1,
                        leadingIcon = Icons.Default.AccountTree
                    )
                }
            }

            // About Section Link
            item {
                SettingsSection(title = stringResource(R.string.section_info)) {
                    SegmentedListItem(
                        onClick = onNavigateToAbout,
                        shapes = expressiveSegmentedShapes(index = 0, count = 1),
                        content = {
                            Text(
                                text = stringResource(R.string.about_headline),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.about_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        trailingContent = {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.height(IntrinsicSize.Min)
                    )
                }
            }
        }
    }
}
