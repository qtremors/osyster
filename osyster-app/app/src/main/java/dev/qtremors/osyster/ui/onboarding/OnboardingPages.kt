package dev.qtremors.osyster.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.R
import dev.qtremors.osyster.ui.expressive.GroupPosition
import dev.qtremors.osyster.ui.theme.expressiveSegmentedShapes

@Composable
fun OnboardingPageContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun OnboardingPageHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeatureHighlightRow(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    SegmentedListItem(
        shapes = expressiveSegmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun OnboardingWelcomePage(
    modifier: Modifier = Modifier
) {
    OnboardingPageContainer(modifier = modifier) {
        OnboardingPageHeader(
            icon = Icons.Default.Speed,
            title = stringResource(R.string.onboarding_welcome_title),
            subtitle = stringResource(R.string.onboarding_welcome_subtitle)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            FeatureHighlightRow(
                index = 0,
                count = 4,
                icon = Icons.Default.CloudOff,
                title = stringResource(R.string.onboarding_feature_offline),
                description = stringResource(R.string.onboarding_feature_offline_desc)
            )
            FeatureHighlightRow(
                index = 1,
                count = 4,
                icon = Icons.Default.Memory,
                title = stringResource(R.string.onboarding_feature_kernel),
                description = stringResource(R.string.onboarding_feature_kernel_desc)
            )
            FeatureHighlightRow(
                index = 2,
                count = 4,
                icon = Icons.Default.Thermostat,
                title = stringResource(R.string.onboarding_feature_thermal),
                description = stringResource(R.string.onboarding_feature_thermal_desc)
            )
            FeatureHighlightRow(
                index = 3,
                count = 4,
                icon = Icons.Default.Code,
                title = stringResource(R.string.onboarding_feature_transparent),
                description = stringResource(R.string.onboarding_feature_transparent_desc)
            )
        }
    }
}

@Composable
fun OnboardingOptionCard(
    title: String,
    description: String,
    pros: List<String>,
    cons: List<String>,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        label = "opt_border"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "opt_container"
    )

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            pros.forEach { pro ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pro,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            cons.forEach { con ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = con,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingStatsPage(
    preferSystemUsageHistory: Boolean,
    onSelectPreferSystem: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingPageContainer(modifier = modifier) {
        OnboardingPageHeader(
            icon = Icons.Outlined.QueryStats,
            title = stringResource(R.string.onboarding_stats_title),
            subtitle = stringResource(R.string.onboarding_stats_subtitle)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OnboardingOptionCard(
                title = stringResource(R.string.onboarding_stats_fresh_title),
                description = stringResource(R.string.onboarding_stats_fresh_desc),
                pros = listOf(
                    stringResource(R.string.onboarding_stats_fresh_pro1),
                    stringResource(R.string.onboarding_stats_fresh_pro2)
                ),
                cons = listOf(
                    stringResource(R.string.onboarding_stats_fresh_con1),
                    stringResource(R.string.onboarding_stats_fresh_con2)
                ),
                isSelected = !preferSystemUsageHistory,
                onSelect = { onSelectPreferSystem(false) }
            )

            OnboardingOptionCard(
                title = stringResource(R.string.onboarding_stats_system_title),
                description = stringResource(R.string.onboarding_stats_system_desc),
                pros = listOf(
                    stringResource(R.string.onboarding_stats_system_pro1),
                    stringResource(R.string.onboarding_stats_system_pro2)
                ),
                cons = listOf(
                    stringResource(R.string.onboarding_stats_system_con1),
                    stringResource(R.string.onboarding_stats_system_con2)
                ),
                isSelected = preferSystemUsageHistory,
                onSelect = { onSelectPreferSystem(true) }
            )
        }
    }
}

@Composable
fun OnboardingPermissionsPage(
    hasUsageAccessPermission: Boolean,
    onRequestUsageAccessPermission: () -> Unit,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    hasNotificationAccessPermission: Boolean = false,
    onRequestNotificationAccessPermission: () -> Unit = {},
    isIgnoringBatteryOptimizations: Boolean,
    onRequestBatteryOptimization: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stabilityExpanded by remember { mutableStateOf(false) }

    OnboardingPageContainer(modifier = modifier) {
        OnboardingPageHeader(
            icon = Icons.Outlined.Security,
            title = stringResource(R.string.onboarding_permissions_setup_title),
            subtitle = stringResource(R.string.onboarding_permissions_setup_subtitle)
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            PermissionCardRow(
                title = stringResource(R.string.onboarding_perm_usage_title),
                description = stringResource(R.string.onboarding_perm_usage_desc),
                icon = Icons.Outlined.BarChart,
                isGranted = hasUsageAccessPermission,
                position = GroupPosition.Top,
                onClick = onRequestUsageAccessPermission
            )

            Spacer(modifier = Modifier.height(4.dp))

            PermissionCardRow(
                title = stringResource(R.string.perm_notifications_title),
                description = stringResource(R.string.perm_notifications_desc),
                icon = Icons.Outlined.Notifications,
                isGranted = hasNotificationPermission,
                position = GroupPosition.Middle,
                onClick = onRequestNotificationPermission
            )

            Spacer(modifier = Modifier.height(4.dp))

            PermissionCardRow(
                title = stringResource(R.string.perm_music_listener_title),
                description = stringResource(R.string.perm_music_listener_desc),
                icon = Icons.Outlined.MusicNote,
                isGranted = hasNotificationAccessPermission,
                position = GroupPosition.Bottom,
                onClick = onRequestNotificationAccessPermission
            )

            Spacer(modifier = Modifier.height(16.dp))

            CollapsiblePermissionSection(
                title = stringResource(R.string.perm_stability_title),
                icon = Icons.Outlined.AutoGraph,
                expanded = stabilityExpanded,
                onToggle = { stabilityExpanded = !stabilityExpanded },
                isAllGranted = isIgnoringBatteryOptimizations
            ) {
                PermissionCardRow(
                    title = stringResource(R.string.perm_battery_opt_title),
                    description = stringResource(R.string.perm_battery_opt_desc),
                    icon = Icons.Outlined.BatteryStd,
                    isGranted = isIgnoringBatteryOptimizations,
                    position = GroupPosition.Single,
                    onClick = onRequestBatteryOptimization
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.onboarding_permissions_optional_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
