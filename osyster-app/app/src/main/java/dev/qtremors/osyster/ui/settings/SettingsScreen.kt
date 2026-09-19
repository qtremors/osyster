@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import dev.qtremors.osyster.ui.onboarding.PermissionBottomSheet
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.BuildConfig
import dev.qtremors.osyster.R
import dev.qtremors.osyster.appinfo.AboutBuildInfo
import dev.qtremors.osyster.appinfo.AboutExternalLink
import dev.qtremors.osyster.appinfo.deviceDescription
import dev.qtremors.osyster.settings.DiagnosticsInterval
import dev.qtremors.osyster.settings.OsysterPreferencesManager
import dev.qtremors.osyster.settings.OsysterPreferencesState
import dev.qtremors.osyster.settings.PreferencesBackupManager
import dev.qtremors.osyster.settings.TemperatureUnit
import dev.qtremors.osyster.ui.theme.bounceClickable
import dev.qtremors.osyster.ui.theme.expressiveSegmentedShapes
import dev.qtremors.osyster.ui.util.OsysterHapticUtil
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: OsysterPreferencesState,
    manager: OsysterPreferencesManager,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToLicenses: () -> Unit = {},
    onNavigateToLicenseDocument: () -> Unit = {},
    backupManager: PreferencesBackupManager? = null,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val buildInfo = remember {
        AboutBuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            applicationId = BuildConfig.APPLICATION_ID,
            buildType = BuildConfig.BUILD_TYPE
        )
    }
    val device = remember { deviceDescription(Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE) }

    val copyText: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    val openLink: (AboutExternalLink) -> Unit = { link -> uriHandler.openUri(link.url) }

    val scrollState = rememberScrollState()
    val overscrollOffset = remember { Animatable(0f) }
    var showPermissionSheet by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && overscrollOffset.value > 0) {
                    val toConsume = if (overscrollOffset.value + available.y >= 0) available.y else -overscrollOffset.value
                    coroutineScope.launch { overscrollOffset.snapTo(overscrollOffset.value + toConsume) }
                    return Offset(0f, toConsume)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && scrollState.value == 0) {
                    val newOffset = (overscrollOffset.value + available.y * 0.5f).coerceAtMost(350f)
                    coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }

    var isAnimatingIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isAnimatingIn = true }

    val contentAlphaState = animateFloatAsState(
        targetValue = if (isAnimatingIn) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = EaseOut),
        label = "content_alpha"
    )
    val contentOffsetState = animateDpAsState(
        targetValue = if (isAnimatingIn) 0.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "content_offset"
    )

    val layoutDirection = LocalLayoutDirection.current
    val effectivePadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
        top = contentPadding.calculateTopPadding() + 8.dp,
        end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
        bottom = contentPadding.calculateBottomPadding() + 96.dp
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release && overscrollOffset.value > 0) {
                            coroutineScope.launch {
                                overscrollOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    }
                }
            }
            .verticalScroll(scrollState)
            .padding(effectivePadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Settings Hero Header with App Icon and Quick Action Buttons
        SettingsHero(
            buildInfo = buildInfo,
            device = device,
            onCopyText = copyText,
            overscrollOffset = overscrollOffset.value,
            contentAlpha = { contentAlphaState.value },
            contentOffset = { contentOffsetState.value },
            onOpenGitHub = { openLink(AboutExternalLink.REPOSITORY) },
            onOpenReleases = { openLink(AboutExternalLink.RELEASES) },
            onOpenIssues = { openLink(AboutExternalLink.REPORT_ISSUE) },
            onCheckUpdates = { openLink(AboutExternalLink.RELEASES) },
            onOpenPrivacy = { openLink(AboutExternalLink.PRIVACY) },
            onOpenNotices = onNavigateToLicenses,
            onOpenLicense = onNavigateToLicenseDocument,
            isCheckingUpdates = false
        )

        // 2. Diagnostics & Telemetry Section
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = contentAlphaState.value
                translationY = contentOffsetState.value.toPx()
            }
        ) {
            SettingsSection(title = stringResource(R.string.section_diagnostics)) {
                SettingsCardContainer(index = 0, count = 3) {
                    DiagnosticsIntervalSelector(
                        currentInterval = state.diagnosticsInterval,
                        onIntervalSelected = manager::setDiagnosticsInterval
                    )
                }

                SettingsCardContainer(index = 1, count = 3) {
                    TemperatureUnitSelector(
                        currentUnit = state.temperatureUnit,
                        onUnitSelected = manager::setTemperatureUnit
                    )
                }

                SettingsSwitchRow(
                    title = stringResource(R.string.process_filter_title),
                    description = stringResource(R.string.process_filter_description),
                    checked = state.showKernelThreads,
                    onCheckedChange = manager::setShowKernelThreads,
                    index = 2,
                    count = 3,
                    leadingIcon = Icons.Default.AccountTree
                )
            }
        }

        // 3. Appearance Section
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = contentAlphaState.value
                translationY = contentOffsetState.value.toPx()
            }
        ) {
            val hasDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val switchCount = if (hasDynamicColorSupport) 2 else 1

            SettingsSection(title = stringResource(R.string.section_appearance)) {
                SettingsCardContainer(index = 0, count = 1 + switchCount) {
                    ThemeModeSelector(
                        currentMode = state.themeMode,
                        onModeSelected = manager::setThemeMode
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    AccentPaletteSelector(
                        currentPalette = state.accentPalette,
                        onPaletteSelected = manager::setAccentPalette
                    )
                }

                if (hasDynamicColorSupport) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.dynamic_color),
                        description = stringResource(R.string.dynamic_color_description),
                        checked = state.dynamicColor,
                        onCheckedChange = manager::setDynamicColor,
                        index = 1,
                        count = 1 + switchCount,
                        leadingIcon = Icons.Default.ColorLens
                    )
                }
                SettingsSwitchRow(
                    title = stringResource(R.string.haptic_feedback),
                    description = stringResource(R.string.haptic_feedback_description),
                    checked = state.hapticFeedback,
                    onCheckedChange = manager::setHapticFeedback,
                    index = if (hasDynamicColorSupport) 2 else 1,
                    count = 1 + switchCount,
                    leadingIcon = Icons.Default.Vibration
                )
            }
        }

        // 4. Security & Privacy Section
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = contentAlphaState.value
                translationY = contentOffsetState.value.toPx()
            }
        ) {
            SettingsSection(title = stringResource(R.string.section_security_privacy)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.block_screen_capture),
                    description = stringResource(R.string.block_screen_capture_description),
                    checked = state.blockScreenCapture,
                    onCheckedChange = manager::setBlockScreenCapture,
                    index = 0,
                    count = 2,
                    leadingIcon = Icons.Default.Security
                )
                SegmentedListItem(
                    onClick = {
                        OsysterHapticUtil.performTick(view, state.hapticFeedback)
                        showPermissionSheet = true
                    },
                    shapes = expressiveSegmentedShapes(index = 1, count = 2),
                    content = {
                        Text(
                            text = stringResource(R.string.settings_permissions_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    supportingContent = {
                        Text(stringResource(R.string.settings_permissions_desc))
                    },
                    leadingContent = {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    trailingContent = {
                        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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

        // 5. Backup and Restore Section
        if (backupManager != null) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlphaState.value
                    translationY = contentOffsetState.value.toPx()
                }
            ) {
                BackupRestoreSection(backupManager = backupManager)
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    if (showPermissionSheet) {
        PermissionBottomSheet(
            onDismissRequest = { showPermissionSheet = false }
        )
    }
}

@Composable
private fun SettingsHero(
    buildInfo: AboutBuildInfo,
    device: String = "",
    onCopyText: (String) -> Unit = {},
    overscrollOffset: Float = 0f,
    contentAlpha: () -> Float = { 1f },
    contentOffset: () -> Dp = { 0.dp },
    onOpenGitHub: () -> Unit = {},
    onOpenReleases: () -> Unit = {},
    onOpenIssues: () -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenNotices: () -> Unit = {},
    onOpenLicense: () -> Unit = {},
    isCheckingUpdates: Boolean = false
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val extraSize = with(density) { overscrollOffset.toDp() }
    val extraBottomPadding = with(density) { (overscrollOffset * 0.35f).toDp() }
    val logoDownwardTranslation = overscrollOffset * 0.2f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 28.dp,
                    bottom = 20.dp + extraBottomPadding
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_osyster_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .graphicsLayer {
                        alpha = contentAlpha()
                        translationY = contentOffset().toPx() + logoDownwardTranslation
                        scaleX = 1f + (overscrollOffset / 1200f)
                        scaleY = 1f + (overscrollOffset / 1200f)
                    }
                    .size(180.dp + extraSize)
                    .clip(RoundedCornerShape(44.dp))
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .graphicsLayer {
                    alpha = contentAlpha()
                    translationY = contentOffset().toPx()
                }
                .padding(top = 2.dp)
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .graphicsLayer {
                    alpha = contentAlpha()
                    translationY = contentOffset().toPx()
                }
                .padding(top = 4.dp)
        )

        // Application ID & Version capsule
        Row(
            modifier = Modifier
                .graphicsLayer {
                    alpha = contentAlpha()
                    translationY = contentOffset().toPx()
                }
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val startShape = splitButtonShape(0, 2)
            Surface(
                shape = startShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clip(startShape)
                    .bounceClickable {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onCopyText(buildInfo.displayPackage)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = buildInfo.displayPackage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val endShape = splitButtonShape(1, 2)
            Surface(
                shape = endShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clip(endShape)
                    .bounceClickable {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onCopyText(buildInfo.displayVersion)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalOffer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = buildInfo.displayVersion,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (device.isNotBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = contentAlpha()
                        translationY = contentOffset().toPx()
                    }
                    .padding(top = 6.dp)
                    .clip(CircleShape)
                    .bounceClickable {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onCopyText(device)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = device,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action buttons grouped in SplitButtonGroup style
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = contentAlpha()
                    translationY = contentOffset().toPx()
                }
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group 1: Issues, GitHub, Releases
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupedActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    label = stringResource(R.string.about_action_issues),
                    onClick = {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onOpenIssues()
                    },
                    shape = splitButtonShape(0, 3),
                    modifier = Modifier.weight(1f)
                )
                GroupedActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    label = stringResource(R.string.about_action_github),
                    onClick = {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onOpenGitHub()
                    },
                    shape = splitButtonShape(1, 3),
                    modifier = Modifier.weight(1f)
                )
                GroupedActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.NewReleases,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    label = stringResource(R.string.about_action_releases),
                    onClick = {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onOpenReleases()
                    },
                    shape = splitButtonShape(2, 3),
                    modifier = Modifier.weight(1f)
                )
            }

            // Group 2: Notices, Privacy, License
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupedActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    label = stringResource(R.string.about_action_notices),
                    onClick = {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onOpenNotices()
                    },
                    shape = splitButtonShape(0, 3),
                    modifier = Modifier.weight(1f)
                )
                GroupedActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.PrivacyTip,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    label = stringResource(R.string.about_action_privacy),
                    onClick = {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onOpenPrivacy()
                    },
                    shape = splitButtonShape(1, 3),
                    modifier = Modifier.weight(1f)
                )
                GroupedActionButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Balance,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    label = stringResource(R.string.about_action_license),
                    onClick = {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onOpenLicense()
                    },
                    shape = splitButtonShape(2, 3),
                    modifier = Modifier.weight(1f)
                )
            }

            // Group 3: Updates
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(CircleShape)
                    .bounceClickable(enabled = !isCheckingUpdates) {
                        OsysterHapticUtil.performVirtualKey(view, true)
                        onCheckUpdates()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isCheckingUpdates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(if (isCheckingUpdates) R.string.checking_for_updates else R.string.check_for_updates),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
