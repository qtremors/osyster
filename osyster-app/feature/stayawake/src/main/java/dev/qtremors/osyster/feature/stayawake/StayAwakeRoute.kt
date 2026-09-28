package dev.qtremors.osyster.feature.stayawake

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.qtremors.osyster.core.navigation.StayAwakeRoute

sealed interface StayAwakeDestination {
    data object DeveloperOptions : StayAwakeDestination
}

fun NavGraphBuilder.registerStayAwakeRoute(onNavigate: (StayAwakeDestination) -> Unit) {
    composable<StayAwakeRoute> {
        val viewModel: StayAwakeViewModel = hiltViewModel()
        val timer by viewModel.timer.collectAsStateWithLifecycle()
        val whileCharging by viewModel.whileCharging.collectAsStateWithLifecycle()
        val selected by viewModel.selectedMinutes.collectAsStateWithLifecycle()
        // The countdown notification needs notification permission on Android 13+; the timer
        // runs either way, so a "no" only hides the notification.
        val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.start()
        }
        LifecycleResumeEffect(Unit) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        val canGrant by viewModel.canGrant.collectAsStateWithLifecycle()
        StayAwakeScreen(
            timer = timer,
            selectedMinutes = selected,
            whileCharging = whileCharging,
            adbCommand = viewModel.adbCommand,
            canGrant = canGrant,
            actions = StayAwakeActions(
                onSelect = viewModel::select,
                onStart = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.start()
                    }
                },
                onStop = viewModel::stop,
                onWhileCharging = viewModel::setWhileCharging,
                onOpenDeveloperOptions = { onNavigate(StayAwakeDestination.DeveloperOptions) },
                onGrant = viewModel::grant
            )
        )
    }
}
