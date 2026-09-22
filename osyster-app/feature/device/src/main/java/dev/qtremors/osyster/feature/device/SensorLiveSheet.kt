package dev.qtremors.osyster.feature.device

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.device.domain.SensorAccuracy
import dev.qtremors.osyster.core.device.domain.SensorReading
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterBottomSheet
import dev.qtremors.osyster.core.ui.theme.MetricTextStyle
import kotlinx.coroutines.flow.Flow

/**
 * Live values from one sensor. The sensor is registered while this sheet is on screen and
 * released the moment it closes, so it never drains the battery in the background.
 */
@Composable
internal fun SensorLiveSheet(readings: Flow<SensorReading>, onDismiss: () -> Unit) {
    val reading by readings.collectAsStateWithLifecycle(initialValue = null)
    OsysterBottomSheet(
        onDismissRequest = onDismiss,
        title = reading?.name ?: stringResource(R.string.sensor_live_title)
    ) {
        val current = reading
        if (current == null) {
            LoadingState()
            Text(
                text = stringResource(R.string.sensor_waiting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            return@OsysterBottomSheet
        }
        ConnectedGroup {
            current.values.forEach { value ->
                Surface(color = MaterialTheme.colorScheme.surfaceBright) {
                    Text(
                        text = value,
                        style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.sensor_accuracy, stringResource(accuracyLabel(current.accuracy))),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun accuracyLabel(accuracy: SensorAccuracy): Int = when (accuracy) {
    SensorAccuracy.UNRELIABLE -> R.string.sensor_accuracy_unreliable
    SensorAccuracy.LOW -> R.string.sensor_accuracy_low
    SensorAccuracy.MEDIUM -> R.string.sensor_accuracy_medium
    SensorAccuracy.HIGH -> R.string.sensor_accuracy_high
    SensorAccuracy.UNKNOWN -> R.string.sensor_accuracy_unknown
}
