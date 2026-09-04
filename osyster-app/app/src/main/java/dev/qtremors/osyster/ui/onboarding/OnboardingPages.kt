package dev.qtremors.osyster.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.R
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
fun OnboardingPermissionsPage(
    hasNotificationPermission: Boolean,
    hasUsageAccessPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestUsageAccessPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingPageContainer(modifier = modifier) {
        OnboardingPageHeader(
            icon = Icons.Default.Security,
            title = stringResource(R.string.onboarding_permissions_title),
            subtitle = stringResource(R.string.onboarding_permissions_subtitle)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            OnboardingPermissionRow(
                index = 0,
                count = 2,
                title = stringResource(R.string.onboarding_perm_notifications_title),
                description = stringResource(R.string.onboarding_perm_notifications_desc),
                icon = Icons.Default.Notifications,
                granted = hasNotificationPermission,
                onClick = onRequestNotificationPermission
            )

            OnboardingPermissionRow(
                index = 1,
                count = 2,
                title = stringResource(R.string.onboarding_perm_usage_title),
                description = stringResource(R.string.onboarding_perm_usage_desc),
                icon = Icons.Default.QueryStats,
                granted = hasUsageAccessPermission,
                onClick = onRequestUsageAccessPermission
            )
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
