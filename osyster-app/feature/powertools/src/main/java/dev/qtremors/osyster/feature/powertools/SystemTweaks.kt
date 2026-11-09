package dev.qtremors.osyster.feature.powertools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.privilege.domain.Keys
import dev.qtremors.osyster.core.privilege.domain.PrivateDnsMode
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import dev.qtremors.osyster.core.privilege.domain.SettingKey
import dev.qtremors.osyster.core.privilege.domain.SystemSettings
import dev.qtremors.osyster.core.privilege.domain.isValidDnsHost
import dev.qtremors.osyster.core.privilege.domain.readFlag
import dev.qtremors.osyster.core.ui.components.ChoiceRow
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.ToggleRow
import javax.inject.Inject
import kotlinx.coroutines.launch

/** One on/off system setting. [inverted] is for "disable_" keys shown as a positive switch. */
enum class Tweak(val key: SettingKey, val inverted: Boolean = false, val defaultOn: Boolean = false) {
    DEVELOPER_OPTIONS(Keys.DeveloperOptions),
    USB_DEBUGGING(Keys.UsbDebugging),
    WIRELESS_DEBUGGING(Keys.WirelessDebugging),
    BLUR(Keys.DisableBlurs, inverted = true, defaultOn = true),
    TAP_TO_WAKE(Keys.TapToWake),
    DOUBLE_TAP_TO_WAKE(Keys.DoubleTapToWake),
    HEADS_UP(Keys.HeadsUp, defaultOn = true),
    SHOW_TOUCHES(Keys.ShowTouches),
    POINTER_LOCATION(Keys.PointerLocation)
}

data class TweaksState(
    val on: Map<Tweak, Boolean> = emptyMap(),
    val dnsMode: PrivateDnsMode = PrivateDnsMode.AUTOMATIC,
    val dnsHost: String = ""
)

@HiltViewModel
class SystemTweaksViewModel @Inject constructor(
    shell: PrivilegedShell,
    settings: SystemSettings
) : PowerViewModel(shell, settings) {
    val state = settingsState(Tweak.entries.map(Tweak::key).toSet() + setOf(Keys.PrivateDnsMode, Keys.PrivateDnsHost), TweaksState()) {
        TweaksState(
            on = Tweak.entries.associateWith { tweak ->
                val stored = settings.readFlag(tweak.key, default = if (tweak.inverted) !tweak.defaultOn else tweak.defaultOn)
                if (tweak.inverted) !stored else stored
            },
            dnsMode = PrivateDnsMode.parse(settings.read(Keys.PrivateDnsMode)),
            dnsHost = settings.read(Keys.PrivateDnsHost).orEmpty()
        )
    }

    fun set(tweak: Tweak, on: Boolean) {
        val stored = if (tweak.inverted) !on else on
        write(tweak.key, if (stored) "1" else "0")
    }

    fun setDnsMode(mode: PrivateDnsMode) {
        if (mode == PrivateDnsMode.HOSTNAME && !isValidDnsHost(state.value.dnsHost)) {
            say(PowerMessage.INVALID_HOST)
            return
        }
        write(Keys.PrivateDnsMode, mode.value)
    }

    /** Saves the provider and switches to it, so typing a host and tapping Save is all it takes. */
    fun saveDnsHost(host: String) {
        val trimmed = host.trim()
        if (!isValidDnsHost(trimmed)) {
            say(PowerMessage.INVALID_HOST)
            return
        }
        viewModelScope.launch {
            if (writeNow(Keys.PrivateDnsHost, trimmed)) writeNow(Keys.PrivateDnsMode, PrivateDnsMode.HOSTNAME.value)
        }
    }
}

@Composable
internal fun SystemTweaksRoute(onNavigate: (PowerToolsDestination) -> Unit) {
    val viewModel: SystemTweaksViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val access by viewModel.access.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val snackbar = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbar)
    val enabled = access.canWriteSettings
    OsysterScaffold(title = stringResource(R.string.tweaks_title), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "gate") { AccessCard(access, needsShell = false) { onNavigate(PowerToolsDestination.Setup) } }
            TWEAK_GROUPS.forEach { (title, tweaks) ->
                item(key = title) {
                    SectionHeader(stringResource(title))
                    ConnectedGroup {
                        tweaks.forEach { tweak ->
                            ToggleRow(
                                title = stringResource(tweak.title),
                                supporting = stringResource(tweak.summary),
                                checked = state.on[tweak] ?: tweak.defaultOn,
                                onCheckedChange = { viewModel.set(tweak, it) },
                                icon = tweak.icon,
                                badgeKey = tweak.name,
                                enabled = enabled
                            )
                        }
                    }
                }
            }
            item(key = "dns") { PrivateDnsSection(state, enabled, viewModel::setDnsMode, viewModel::saveDnsHost) }
        }
    }
}

@Composable
private fun PrivateDnsSection(state: TweaksState, enabled: Boolean, onMode: (PrivateDnsMode) -> Unit, onSave: (String) -> Unit) {
    var host by rememberSaveable(state.dnsHost) { mutableStateOf(state.dnsHost) }
    SectionHeader(stringResource(R.string.tweaks_group_network))
    ConnectedGroup {
        ChoiceRow(
            title = stringResource(R.string.tweaks_private_dns),
            supporting = stringResource(R.string.tweaks_private_dns_summary),
            options = PrivateDnsMode.entries,
            selected = state.dnsMode,
            label = { mode -> stringResource(mode.label) },
            onSelect = { if (enabled) onMode(it) },
            icon = Icons.Rounded.Dns
        )
        Surface(color = MaterialTheme.colorScheme.surfaceBright, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.take(MAX_HOST) },
                    label = { Text(stringResource(R.string.tweaks_private_dns_host)) },
                    placeholder = { Text(stringResource(R.string.tweaks_private_dns_example)) },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { onSave(host) }, enabled = enabled && host.isNotBlank()) {
                    Text(stringResource(R.string.tweaks_private_dns_save))
                }
            }
        }
    }
}

private val TWEAK_GROUPS = listOf(
    R.string.tweaks_group_debugging to listOf(Tweak.DEVELOPER_OPTIONS, Tweak.USB_DEBUGGING, Tweak.WIRELESS_DEBUGGING),
    R.string.tweaks_group_display to listOf(Tweak.BLUR, Tweak.TAP_TO_WAKE, Tweak.DOUBLE_TAP_TO_WAKE),
    R.string.tweaks_group_input to listOf(Tweak.HEADS_UP, Tweak.SHOW_TOUCHES, Tweak.POINTER_LOCATION)
)

private const val MAX_HOST = 253

private val PrivateDnsMode.label: Int
    get() = when (this) {
        PrivateDnsMode.OFF -> R.string.tweaks_dns_off
        PrivateDnsMode.AUTOMATIC -> R.string.tweaks_dns_automatic
        PrivateDnsMode.HOSTNAME -> R.string.tweaks_dns_provider
    }

private val Tweak.title: Int
    get() = when (this) {
        Tweak.DEVELOPER_OPTIONS -> R.string.tweak_developer_options
        Tweak.USB_DEBUGGING -> R.string.tweak_usb_debugging
        Tweak.WIRELESS_DEBUGGING -> R.string.tweak_wireless_debugging
        Tweak.BLUR -> R.string.tweak_blur
        Tweak.TAP_TO_WAKE -> R.string.tweak_tap_to_wake
        Tweak.DOUBLE_TAP_TO_WAKE -> R.string.tweak_double_tap_to_wake
        Tweak.HEADS_UP -> R.string.tweak_heads_up
        Tweak.SHOW_TOUCHES -> R.string.tweak_show_touches
        Tweak.POINTER_LOCATION -> R.string.tweak_pointer_location
    }

private val Tweak.summary: Int
    get() = when (this) {
        Tweak.DEVELOPER_OPTIONS -> R.string.tweak_developer_options_summary
        Tweak.USB_DEBUGGING -> R.string.tweak_usb_debugging_summary
        Tweak.WIRELESS_DEBUGGING -> R.string.tweak_wireless_debugging_summary
        Tweak.BLUR -> R.string.tweak_blur_summary
        Tweak.TAP_TO_WAKE -> R.string.tweak_tap_to_wake_summary
        Tweak.DOUBLE_TAP_TO_WAKE -> R.string.tweak_double_tap_to_wake_summary
        Tweak.HEADS_UP -> R.string.tweak_heads_up_summary
        Tweak.SHOW_TOUCHES -> R.string.tweak_show_touches_summary
        Tweak.POINTER_LOCATION -> R.string.tweak_pointer_location_summary
    }

private val Tweak.icon
    get() = when (this) {
        Tweak.DEVELOPER_OPTIONS -> Icons.Rounded.DeveloperMode
        Tweak.USB_DEBUGGING -> Icons.Rounded.Usb
        Tweak.WIRELESS_DEBUGGING -> Icons.Rounded.Wifi
        Tweak.BLUR -> Icons.Rounded.BlurOn
        Tweak.TAP_TO_WAKE, Tweak.DOUBLE_TAP_TO_WAKE, Tweak.SHOW_TOUCHES, Tweak.POINTER_LOCATION -> Icons.Rounded.TouchApp
        Tweak.HEADS_UP -> Icons.Rounded.Notifications
    }
