package dev.qtremors.osyster.feature.onboarding

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge

/**
 * Two pages: what Osyster is with its privacy promises, then the optional access a few tools use.
 * Drawn on a Surface so every text color follows the theme, including dark and pure black.
 */
@Composable
internal fun WelcomeScreen(access: AccessStatus, onNavigate: (OnboardingDestination) -> Unit, onFinish: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = page > 0) { page = 0 }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.safeDrawingPadding()) {
            AnimatedContent(targetState = page, label = "welcomePage", modifier = Modifier.fillMaxSize()) { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (current == 0) Intro() else Access(access, onNavigate)
                }
            }
            Footer(page = page, onSkip = onFinish, onNext = { if (page == 0) page = 1 else onFinish() }, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Skip on the left (first page only), page dots in the exact middle, and the main button on the right. */
@Composable
private fun Footer(page: Int, onSkip: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(24.dp)) {
        if (page < PAGES - 1) {
            TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterStart)) { Text(stringResource(R.string.welcome_skip)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.Center)) {
            repeat(PAGES) { index ->
                Box(
                    Modifier.size(if (index == page) 10.dp else 8.dp).background(
                        if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
                )
            }
        }
        Button(onClick = onNext, modifier = Modifier.align(Alignment.CenterEnd)) {
            Text(stringResource(if (page == 0) R.string.welcome_next else R.string.welcome_done))
        }
    }
}

@Composable
private fun Intro() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(128.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center
        ) { AppIcon(context.packageName, size = 80.dp) }
        Text(
            stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            stringResource(R.string.welcome_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
    ConnectedGroup {
        ConnectedRow(stringResource(R.string.welcome_offline), supporting = stringResource(R.string.welcome_offline_detail), leading = { IconBadge(Icons.Rounded.CloudOff, key = "offline") })
        ConnectedRow(stringResource(R.string.welcome_local), supporting = stringResource(R.string.welcome_local_detail), leading = { IconBadge(Icons.Rounded.Storage, key = "local") })
        ConnectedRow(stringResource(R.string.welcome_ids), supporting = stringResource(R.string.welcome_ids_detail), leading = { IconBadge(Icons.Rounded.VisibilityOff, key = "ids") })
    }
}

@Composable
private fun Access(access: AccessStatus, onNavigate: (OnboardingDestination) -> Unit) {
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(notificationsGranted(context)) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
    }
    Text(
        stringResource(R.string.welcome_access_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 24.dp)
    )
    Text(
        stringResource(R.string.welcome_access_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    ConnectedGroup {
        AccessRow(R.string.welcome_usage, R.string.welcome_usage_detail, Icons.Rounded.QueryStats, access.usage) {
            onNavigate(OnboardingDestination.UsageAccess)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AccessRow(R.string.welcome_notifications, R.string.welcome_notifications_detail, Icons.Rounded.Notifications, granted = notificationsAllowed) {
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        AccessRow(R.string.welcome_notification_access, R.string.welcome_notification_access_detail, Icons.Rounded.NotificationsActive, access.notificationAccess) {
            onNavigate(OnboardingDestination.NotificationAccess)
        }
    }
    Text(
        stringResource(R.string.welcome_access_later),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AccessRow(title: Int, detail: Int, icon: ImageVector, granted: Boolean, onClick: () -> Unit) {
    ConnectedRow(
        title = stringResource(title),
        supporting = stringResource(detail),
        leading = { IconBadge(icon, key = "access_$title") },
        trailing = {
            if (granted) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.welcome_granted), tint = MaterialTheme.colorScheme.primary)
            } else {
                // Says the row is a button before anyone has to guess.
                Text(stringResource(R.string.welcome_allow), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        },
        onClick = if (granted) null else onClick
    )
}

private fun notificationsGranted(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED

private const val PAGES = 2
