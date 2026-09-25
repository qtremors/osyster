package dev.qtremors.osyster.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge

/**
 * Three short steps: what Osyster is, what it promises about privacy, and the optional access
 * that unlocks a few tools. Every step can be skipped; nothing here is required.
 */
@Composable
internal fun WelcomeScreen(access: AccessStatus, onNavigate: (OnboardingDestination) -> Unit, onFinish: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer).safeDrawingPadding()) {
        AnimatedContent(targetState = step, label = "welcomeStep", modifier = Modifier.fillMaxSize()) { current ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (current) {
                    0 -> Intro()
                    1 -> Promises()
                    else -> Access(access, onNavigate)
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.welcome_skip)) }
            Button(onClick = { if (step < LAST_STEP) step++ else onFinish() }) {
                Text(stringResource(if (step < LAST_STEP) R.string.welcome_next else R.string.welcome_done))
            }
        }
    }
}

@Composable
private fun Intro() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(140.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center
        ) { AppIcon(context.packageName, size = 88.dp) }
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.displaySmall)
        Text(
            stringResource(R.string.welcome_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Promises() {
    Text(stringResource(R.string.welcome_privacy_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 32.dp))
    ConnectedGroup {
        ConnectedRow(stringResource(R.string.welcome_offline), supporting = stringResource(R.string.welcome_offline_detail), leading = { IconBadge(Icons.Rounded.CloudOff, key = "offline") })
        ConnectedRow(stringResource(R.string.welcome_local), supporting = stringResource(R.string.welcome_local_detail), leading = { IconBadge(Icons.Rounded.Storage, key = "local") })
        ConnectedRow(stringResource(R.string.welcome_ids), supporting = stringResource(R.string.welcome_ids_detail), leading = { IconBadge(Icons.Rounded.VisibilityOff, key = "ids") })
    }
}

@Composable
private fun Access(access: AccessStatus, onNavigate: (OnboardingDestination) -> Unit) {
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Text(stringResource(R.string.welcome_access_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 32.dp))
    Text(stringResource(R.string.welcome_access_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ConnectedGroup {
        AccessRow(R.string.welcome_usage, R.string.welcome_usage_detail, Icons.Rounded.QueryStats, access.usage) {
            onNavigate(OnboardingDestination.UsageAccess)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AccessRow(R.string.welcome_notifications, R.string.welcome_notifications_detail, Icons.Rounded.Notifications, granted = false) {
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        AccessRow(R.string.welcome_notification_access, R.string.welcome_notification_access_detail, Icons.Rounded.NotificationsActive, access.notificationAccess) {
            onNavigate(OnboardingDestination.NotificationAccess)
        }
    }
    Text(stringResource(R.string.welcome_access_later), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AccessRow(title: Int, detail: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, granted: Boolean, onClick: () -> Unit) {
    ConnectedRow(
        title = stringResource(title),
        supporting = stringResource(detail),
        leading = { IconBadge(icon, key = "access_$title") },
        trailing = { if (granted) Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.welcome_granted), tint = MaterialTheme.colorScheme.primary) },
        onClick = if (granted) null else onClick
    )
}

private const val LAST_STEP = 2
