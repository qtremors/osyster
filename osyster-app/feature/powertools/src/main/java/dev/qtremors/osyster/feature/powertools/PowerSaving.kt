package dev.qtremors.osyster.feature.powertools

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.privilege.domain.BatterySaverConstants
import dev.qtremors.osyster.core.privilege.domain.Keys
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.SaverGroup
import dev.qtremors.osyster.core.privilege.domain.SaverPolicy
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.privilege.domain.readFlag
import dev.qtremors.osyster.core.privilege.domain.readInt
import dev.qtremors.osyster.core.ui.components.ChoiceRow
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import javax.inject.Inject

data class SaverState(
    val on: Boolean = false,
    val triggerLevel: Int = 0,
    val constants: BatterySaverConstants = BatterySaverConstants.parse(null)
)

@HiltViewModel
class PowerSavingViewModel @Inject constructor(
    shell: PrivilegedShell,
    settings: SystemSettings
) : PowerViewModel(shell, settings) {
    val state = settingsState(setOf(Keys.LowPower, Keys.LowPowerTrigger, Keys.BatterySaverConstants), SaverState()) {
        SaverState(
            on = settings.readFlag(Keys.LowPower),
            triggerLevel = settings.readInt(Keys.LowPowerTrigger) ?: 0,
            constants = BatterySaverConstants.parse(settings.read(Keys.BatterySaverConstants))
        )
    }

    fun setOn(on: Boolean) = write(Keys.LowPower, if (on) "1" else "0")

    fun setTrigger(level: Int) = write(Keys.LowPowerTrigger, level.toString())

    fun setPolicy(policy: SaverPolicy, on: Boolean) =
        write(Keys.BatterySaverConstants, state.value.constants.with(policy, on).format())

    /** Removes every override, so battery saver behaves as the phone shipped. */
    fun reset() = write(Keys.BatterySaverConstants, null)
}

@Composable
internal fun PowerSavingRoute(onNavigate: (PowerToolsDestination) -> Unit) {
    val viewModel: PowerSavingViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val access by viewModel.access.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbar)
    val enabled = access.canWriteSettings
    OsysterScaffold(title = stringResource(R.string.power_saving_title), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "gate") { AccessCard(access, needsShell = false) { onNavigate(PowerToolsDestination.Setup) } }
            item(key = "main") {
                ConnectedGroup {
                    ToggleRow(
                        title = stringResource(R.string.power_saving_on),
                        supporting = stringResource(R.string.power_saving_on_summary),
                        checked = state.on,
                        onCheckedChange = viewModel::setOn,
                        icon = Icons.Rounded.BatterySaver,
                        badgeKey = "saver",
                        enabled = enabled
                    )
                    ChoiceRow(
                        title = stringResource(R.string.power_saving_trigger),
                        supporting = stringResource(R.string.power_saving_trigger_summary),
                        options = TRIGGER_LEVELS,
                        selected = TRIGGER_LEVELS.minBy { kotlin.math.abs(it - state.triggerLevel) },
                        label = { level -> if (level == 0) stringResource(R.string.power_saving_trigger_never) else stringResource(R.string.power_percent, level) },
                        onSelect = { if (enabled) viewModel.setTrigger(it) }
                    )
                }
            }
            SaverGroup.entries.forEach { group ->
                item(key = group.name) {
                    SectionHeader(stringResource(group.title))
                    ConnectedGroup {
                        SaverPolicy.entries.filter { it.group == group }.forEach { policy ->
                            ToggleRow(
                                title = stringResource(policy.title),
                                supporting = stringResource(policy.summary),
                                checked = state.constants.flag(policy),
                                onCheckedChange = { viewModel.setPolicy(policy, it) },
                                enabled = enabled
                            )
                        }
                    }
                }
            }
            item(key = "reset") {
                ActionButtons {
                    OutlinedButton(onClick = viewModel::reset, enabled = enabled && !state.constants.isEmpty()) {
                        Text(stringResource(R.string.power_saving_reset))
                    }
                }
            }
        }
    }
}

private val TRIGGER_LEVELS = listOf(0, 5, 10, 15, 20, 30)

private val SaverGroup.title: Int
    get() = when (this) {
        SaverGroup.DISPLAY -> R.string.saver_group_display
        SaverGroup.PERFORMANCE -> R.string.saver_group_performance
        SaverGroup.NETWORK -> R.string.saver_group_network
        SaverGroup.SOUND -> R.string.saver_group_sound
    }

private val SaverPolicy.title: Int
    get() = when (this) {
        SaverPolicy.DISABLE_ANIMATION -> R.string.saver_disable_animation
        SaverPolicy.DISABLE_AOD -> R.string.saver_disable_aod
        SaverPolicy.ENABLE_NIGHT_MODE -> R.string.saver_night_mode
        SaverPolicy.DISABLE_LAUNCH_BOOST -> R.string.saver_launch_boost
        SaverPolicy.ENABLE_QUICK_DOZE -> R.string.saver_quick_doze
        SaverPolicy.FORCE_ALL_APPS_STANDBY -> R.string.saver_all_standby
        SaverPolicy.FORCE_BACKGROUND_CHECK -> R.string.saver_background_check
        SaverPolicy.ENABLE_DATASAVER -> R.string.saver_data_saver
        SaverPolicy.ENABLE_FIREWALL -> R.string.saver_firewall
        SaverPolicy.DEFER_FULL_BACKUP -> R.string.saver_full_backup
        SaverPolicy.DEFER_KEYVALUE_BACKUP -> R.string.saver_keyvalue_backup
        SaverPolicy.DISABLE_VIBRATION -> R.string.saver_vibration
        SaverPolicy.DISABLE_OPTIONAL_SENSORS -> R.string.saver_sensors
    }

private val SaverPolicy.summary: Int
    get() = when (this) {
        SaverPolicy.DISABLE_ANIMATION -> R.string.saver_disable_animation_summary
        SaverPolicy.DISABLE_AOD -> R.string.saver_disable_aod_summary
        SaverPolicy.ENABLE_NIGHT_MODE -> R.string.saver_night_mode_summary
        SaverPolicy.DISABLE_LAUNCH_BOOST -> R.string.saver_launch_boost_summary
        SaverPolicy.ENABLE_QUICK_DOZE -> R.string.saver_quick_doze_summary
        SaverPolicy.FORCE_ALL_APPS_STANDBY -> R.string.saver_all_standby_summary
        SaverPolicy.FORCE_BACKGROUND_CHECK -> R.string.saver_background_check_summary
        SaverPolicy.ENABLE_DATASAVER -> R.string.saver_data_saver_summary
        SaverPolicy.ENABLE_FIREWALL -> R.string.saver_firewall_summary
        SaverPolicy.DEFER_FULL_BACKUP -> R.string.saver_full_backup_summary
        SaverPolicy.DEFER_KEYVALUE_BACKUP -> R.string.saver_keyvalue_backup_summary
        SaverPolicy.DISABLE_VIBRATION -> R.string.saver_vibration_summary
        SaverPolicy.DISABLE_OPTIONAL_SENSORS -> R.string.saver_sensors_summary
    }
