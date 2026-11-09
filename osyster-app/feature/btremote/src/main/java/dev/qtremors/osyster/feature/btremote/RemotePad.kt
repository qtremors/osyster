package dev.qtremors.osyster.feature.btremote

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.hid.domain.RemoteKey
import dev.qtremors.osyster.core.hid.domain.LayoutChoice
import dev.qtremors.osyster.core.hid.domain.RemoteLayout
import dev.qtremors.osyster.core.hid.domain.layoutFor
import dev.qtremors.osyster.core.hid.domain.shows
import dev.qtremors.osyster.core.hid.domain.RemoteNavigation
import dev.qtremors.osyster.core.hid.domain.RemoteSettings
import dev.qtremors.osyster.core.hid.domain.swipeDirection
import dev.qtremors.osyster.core.ui.components.MenuAction
import dev.qtremors.osyster.core.ui.components.OsysterMenu
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics

/** The connected remote: top keys, D-pad or swipe pad, volume and channel, then media and color keys. */
@Composable
internal fun RemoteContent(viewModel: RemoteViewModel, settings: RemoteSettings, padding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val layoutChoice by viewModel.layoutChoice.collectAsStateWithLifecycle()
    var channelPadOpen by rememberSaveable { mutableStateOf(false) }
    val name = state.device?.name?.ifBlank { null } ?: stringResource(R.string.remote_unnamed)
    val keys = KeyActions(onPress = viewModel::hold, onRelease = viewModel::release)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConnectedChip(
            name = name,
            layout = stringResource(layout.label),
            icon = state.device?.kind?.icon,
            choice = layoutChoice,
            detected = stringResource(layoutFor(LayoutChoice.AUTO, state.device?.kind).label),
            onChoose = { choice -> state.device?.let { viewModel.setLayout(it, choice) } }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (!settings.minimal && layout.shows(RemoteKey.POWER)) {
                RemoteKeyButton(
                    Icons.Rounded.PowerSettingsNew, stringResource(R.string.remote_power), keys, RemoteKey.POWER, Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.errorContainer, content = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            RemoteKeyButton(Icons.Rounded.Home, stringResource(R.string.remote_home), keys, RemoteKey.HOME, Modifier.weight(1f))
            RemoteKeyButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.remote_back), keys, RemoteKey.BACK, Modifier.weight(1f))
            if (!settings.minimal && layout.shows(RemoteKey.MENU)) {
                RemoteKeyButton(Icons.Rounded.Menu, stringResource(R.string.remote_menu), keys, RemoteKey.MENU, Modifier.weight(1f))
            }
        }

        when (settings.navigation) {
            RemoteNavigation.DPAD -> DPad(keys = keys, onSelectPress = { viewModel.hold(RemoteKey.SELECT) }, onSelectRelease = viewModel::release)
            RemoteNavigation.SWIPE -> SwipePad(keys = keys, onSelectPress = { viewModel.hold(RemoteKey.SELECT) }, onSelectRelease = viewModel::release)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Rocker(
                label = stringResource(R.string.remote_volume),
                keys = keys,
                down = RemoteKey.VOLUME_DOWN to stringResource(R.string.remote_volume_down),
                middle = Triple(Icons.Rounded.VolumeOff, stringResource(R.string.remote_mute), { viewModel.tap(RemoteKey.MUTE) }),
                up = RemoteKey.VOLUME_UP to stringResource(R.string.remote_volume_up),
                modifier = Modifier.weight(1f)
            )
            if (!settings.minimal && layout.shows(RemoteKey.CHANNEL_UP)) {
                Rocker(
                    label = stringResource(R.string.remote_channel),
                    keys = keys,
                    down = RemoteKey.CHANNEL_DOWN to stringResource(R.string.remote_channel_down),
                    middle = Triple(Icons.Rounded.Dialpad, stringResource(R.string.remote_channel_pad), { channelPadOpen = true }),
                    up = RemoteKey.CHANNEL_UP to stringResource(R.string.remote_channel_up),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!settings.minimal) MediaRow(keys = keys)
        if (!settings.minimal && layout.shows(RemoteKey.RED)) ColorKeys(keys = keys)
    }

    if (channelPadOpen) ChannelPadSheet(onKey = viewModel::typeKey, onDismiss = { channelPadOpen = false })
}

/** Press and release callbacks for remote keys, so every key button shares one pair. */
internal class KeyActions(val onPress: (RemoteKey) -> Unit, val onRelease: () -> Unit)

/**
 * Who is connected and which remote is showing. Tap to pick another layout for this device; the
 * choice is saved and used every time it connects.
 */
@Composable
private fun ConnectedChip(
    name: String,
    layout: String,
    icon: ImageVector?,
    choice: LayoutChoice,
    detected: String,
    onChoose: (LayoutChoice) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { menuOpen = true },
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
            ) {
                if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.remote_connected_layout, name, layout), style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        OsysterMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            groups = listOf(
                LayoutChoice.entries.map { option ->
                    val label = if (option == LayoutChoice.AUTO) stringResource(R.string.remote_layout_auto_detected, detected) else stringResource(option.label)
                    MenuAction(label, selected = option == choice) { onChoose(option) }
                }
            )
        )
    }
}

/**
 * A remote key: pressed on touch, released on lift, so holding repeats on the TV like a real
 * remote. It squishes and scales on press as Expressive feedback. TalkBack can tap it like a button.
 */
@Composable
internal fun PressKey(
    icon: ImageVector,
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurface,
    iconSize: Dp = 26.dp
) {
    PressKey(label, onPress, onRelease, modifier, shape, container) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
    }
}

/** [PressKey] with any content, such as the OK label or a color key's color bar. */
@Composable
internal fun PressKey(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable () -> Unit
) {
    val haptics = rememberAppHaptics()
    val press by rememberUpdatedState(onPress)
    val release by rememberUpdatedState(onRelease)
    var pressed by remember { mutableStateOf(false) }
    val corner by animateFloatAsState(if (pressed) 28f else 50f, spring(stiffness = 900f), label = "keyCorner")
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "keyScale")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape ?: RoundedCornerShape(percent = corner.toInt()))
            .background(container)
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick(label) {
                    press()
                    release()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    haptics.tap()
                    press()
                    tryAwaitRelease()
                    pressed = false
                    release()
                })
            }
    ) {
        content()
    }
}

@Composable
private fun RemoteKeyButton(
    icon: ImageVector,
    label: String,
    keys: KeyActions,
    key: RemoteKey,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurface
) {
    PressKey(icon, label, { keys.onPress(key) }, keys.onRelease, modifier.height(56.dp), container = container, content = content)
}

/** Four arrows around a cookie-shaped OK, inside one round pad. */
@Composable
private fun DPad(keys: KeyActions, onSelectPress: () -> Unit, onSelectRelease: () -> Unit) {
    val arrow = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(268.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        val arrowSize = Modifier.size(76.dp)
        PressKey(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.remote_up), { keys.onPress(RemoteKey.UP) }, keys.onRelease,
            arrowSize.align(Alignment.TopCenter).padding(top = 6.dp), container = arrow, iconSize = 34.dp)
        PressKey(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.remote_down), { keys.onPress(RemoteKey.DOWN) }, keys.onRelease,
            arrowSize.align(Alignment.BottomCenter).padding(bottom = 6.dp), container = arrow, iconSize = 34.dp)
        PressKey(Icons.Rounded.KeyboardArrowLeft, stringResource(R.string.remote_left), { keys.onPress(RemoteKey.LEFT) }, keys.onRelease,
            arrowSize.align(Alignment.CenterStart).padding(start = 6.dp), container = arrow, iconSize = 34.dp)
        PressKey(Icons.Rounded.KeyboardArrowRight, stringResource(R.string.remote_right), { keys.onPress(RemoteKey.RIGHT) }, keys.onRelease,
            arrowSize.align(Alignment.CenterEnd).padding(end = 6.dp), container = arrow, iconSize = 34.dp)
        OkKey(onSelectPress, onSelectRelease, Modifier.size(108.dp))
    }
}

@Composable
private fun OkKey(onPress: () -> Unit, onRelease: () -> Unit, modifier: Modifier) {
    val label = stringResource(R.string.remote_ok)
    PressKey(label, onPress, onRelease, modifier, MaterialShapes.Cookie9Sided.toShape(), MaterialTheme.colorScheme.primary) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
    }
}

/**
 * Swipe to move: the first [SWIPE_START_PX] of movement picks a direction and holds that arrow
 * until the finger lifts, so a long swipe keeps scrolling. A touch that never moves is OK.
 */
@Composable
private fun SwipePad(keys: KeyActions, onSelectPress: () -> Unit, onSelectRelease: () -> Unit) {
    val haptics = rememberAppHaptics()
    val press by rememberUpdatedState(keys.onPress)
    val release by rememberUpdatedState(keys.onRelease)
    val select by rememberUpdatedState(onSelectPress)
    val selectRelease by rememberUpdatedState(onSelectRelease)
    val label = stringResource(R.string.remote_swipe_hint)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .clip(RoundedCornerShape(44.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var held: RemoteKey? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (held == null) {
                            val moved = change.position - down.position
                            swipeDirection(moved.x, moved.y, SWIPE_START_PX)?.let { key ->
                                held = key
                                haptics.tick()
                                press(key)
                            }
                        }
                    }
                    if (held != null) {
                        release()
                    } else {
                        haptics.tap()
                        select()
                        selectRelease()
                    }
                }
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Swipe, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A horizontal pill: minus, a middle action, plus. */
@Composable
private fun Rocker(
    label: String,
    keys: KeyActions,
    down: Pair<RemoteKey, String>,
    middle: Triple<ImageVector, String, () -> Unit>,
    up: Pair<RemoteKey, String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp)
        ) {
            PressKey(Icons.Rounded.Remove, down.second, { keys.onPress(down.first) }, keys.onRelease, Modifier.weight(1f).height(52.dp))
            PressKey(middle.first, middle.second, middle.third, {}, Modifier.weight(1f).height(52.dp), container = MaterialTheme.colorScheme.secondaryContainer, content = MaterialTheme.colorScheme.onSecondaryContainer)
            PressKey(Icons.Rounded.Add, up.second, { keys.onPress(up.first) }, keys.onRelease, Modifier.weight(1f).height(52.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MediaRow(keys: KeyActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        MediaKey(Icons.Rounded.SkipPrevious, stringResource(R.string.remote_previous), keys, RemoteKey.PREVIOUS)
        MediaKey(Icons.Rounded.FastRewind, stringResource(R.string.remote_rewind), keys, RemoteKey.REWIND)
        PressKey(
            Icons.Rounded.PlayArrow, stringResource(R.string.remote_play_pause), { keys.onPress(RemoteKey.PLAY_PAUSE) }, keys.onRelease,
            Modifier.weight(1.4f).height(64.dp),
            container = MaterialTheme.colorScheme.primaryContainer, content = MaterialTheme.colorScheme.onPrimaryContainer, iconSize = 32.dp
        )
        MediaKey(Icons.Rounded.FastForward, stringResource(R.string.remote_fast_forward), keys, RemoteKey.FAST_FORWARD)
        MediaKey(Icons.Rounded.SkipNext, stringResource(R.string.remote_next), keys, RemoteKey.NEXT)
    }
}

@Composable
private fun RowScope.MediaKey(icon: ImageVector, label: String, keys: KeyActions, key: RemoteKey) {
    PressKey(icon, label, { keys.onPress(key) }, keys.onRelease, Modifier.weight(1f).height(56.dp))
}

/** Teletext and TV app color keys, plus captions. */
@Composable
private fun ColorKeys(keys: KeyActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            Triple(RemoteKey.RED, R.string.remote_red, Color(0xFFE5484D)),
            Triple(RemoteKey.GREEN, R.string.remote_green, Color(0xFF30A46C)),
            Triple(RemoteKey.YELLOW, R.string.remote_yellow, Color(0xFFF5C518)),
            Triple(RemoteKey.BLUE, R.string.remote_blue, Color(0xFF3E63DD))
        ).forEach { (key, label, color) ->
            ColorKey(stringResource(label), color, { keys.onPress(key) }, keys.onRelease, Modifier.weight(1f))
        }
        PressKey(Icons.Rounded.ClosedCaption, stringResource(R.string.remote_captions), { keys.onPress(RemoteKey.CAPTIONS) }, keys.onRelease, Modifier.weight(1f).height(40.dp), iconSize = 22.dp)
    }
}

@Composable
private fun ColorKey(label: String, color: Color, onPress: () -> Unit, onRelease: () -> Unit, modifier: Modifier) {
    PressKey(label, onPress, onRelease, modifier.height(40.dp)) {
        Box(modifier = Modifier.size(width = 28.dp, height = 8.dp).clip(CircleShape).background(color))
    }
}

internal val RemoteLayout.label: Int
    get() = when (this) {
        RemoteLayout.TV -> R.string.remote_layout_tv
        RemoteLayout.COMPUTER -> R.string.remote_layout_computer
        RemoteLayout.TABLET -> R.string.remote_layout_tablet
    }

private const val SWIPE_START_PX = 24f
