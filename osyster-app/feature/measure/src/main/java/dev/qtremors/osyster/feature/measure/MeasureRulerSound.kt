package dev.qtremors.osyster.feature.measure

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.qtremors.osyster.core.ui.components.ConnectedChoices
import dev.qtremors.osyster.core.ui.components.EmptyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * A ruler along the screen's width at true size, from the display's physical dots per inch.
 * Drag or tap to set the end line; the length shows in centimeters or inches.
 */
@Composable
internal fun RulerTool() {
    val context = LocalContext.current
    val dpi = remember { context.resources.displayMetrics.xdpi }
    var inches by rememberSaveable { mutableStateOf(false) }
    var mark by remember { mutableFloatStateOf(0f) }
    val colors = MaterialTheme.colorScheme
    val length = pixelsToCm(mark, dpi).let { if (inches) it / CM_PER_INCH else it }
    val description = stringResource(R.string.ruler_scale)
    Surface(color = colors.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 24.dp)) {
            Text(
                stringResource(if (inches) R.string.ruler_value_inch else R.string.ruler_value_cm, oneDecimal(length)),
                style = MaterialTheme.typography.displaySmall
            )
            // The scale starts at the very left edge of the screen, so the card ignores its own padding here.
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .semantics { contentDescription = description }
                    .pointerInput(Unit) { detectTapGestures { mark = it.x } }
                    .pointerInput(Unit) { detectDragGestures { change, _ -> mark = change.position.x.coerceIn(0f, size.width.toFloat()) } }
            ) {
                val unitPx = if (inches) dpi else cmToPixels(1f, dpi)
                val steps = if (inches) 8 else 10
                val stepPx = unitPx / steps
                var i = 0
                while (i * stepPx <= size.width) {
                    val x = i * stepPx
                    val major = i % steps == 0
                    val half = i % (steps / 2) == 0
                    val h = size.height * when {
                        major -> 0.55f
                        half -> 0.38f
                        else -> 0.22f
                    }
                    drawLine(colors.onSurfaceVariant, Offset(x, 0f), Offset(x, h), strokeWidth = (if (major) 2 else 1).dp.toPx())
                    i++
                }
                drawLine(colors.primary, Offset(mark, 0f), Offset(mark, size.height), strokeWidth = 3.dp.toPx())
            }
            ConnectedChoices(
                options = listOf(false, true),
                selected = inches,
                label = { stringResource(if (it) R.string.ruler_inch else R.string.ruler_cm) },
                onSelect = { inches = it },
                modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
            )
            Text(stringResource(R.string.ruler_hint), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

/**
 * A sound level meter. The microphone is read in small buffers on a background thread only while
 * this is on screen; each buffer becomes one loudness number and is thrown away.
 */
@Composable
internal fun SoundTool(onAppSettings: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    if (!granted) {
        EmptyState(
            Icons.Rounded.Mic,
            stringResource(R.string.sound_permission_title),
            stringResource(R.string.sound_permission_message),
            actionLabel = stringResource(if (asked) R.string.sound_permission_settings else R.string.sound_permission_action),
            onAction = { if (asked) onAppSettings() else ask.launch(Manifest.permission.RECORD_AUDIO) }
        )
        return
    }
    var level by remember { mutableIntStateOf(0) }
    var peak by remember { mutableIntStateOf(0) }
    Listen { db ->
        level = db
        if (db > peak) peak = db
    }
    val colors = MaterialTheme.colorScheme
    val bands = stringArrayResource(R.array.sound_bands)
    Surface(color = colors.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(24.dp)) {
            Text(stringResource(R.string.sound_level, level), style = MaterialTheme.typography.displayMedium)
            Text(bands[loudnessBand(level.toDouble())], style = MaterialTheme.typography.titleMedium, color = if (level >= LOUD_DB) colors.error else colors.primary)
            Box(modifier = Modifier.fillMaxWidth()) {
                LinearWavyProgressIndicator(progress = { (level / MAX_DB).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
            Text(stringResource(R.string.sound_peak, peak), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            Text(stringResource(R.string.sound_note), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** Reads the microphone while in composition and reports about five loudness values a second. */
@SuppressLint("MissingPermission")
@Composable
private fun Listen(onLevel: (Int) -> Unit) {
    var latest by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val rate = SAMPLE_RATE
            val size = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(rate / 5)
            val recorder = runCatching { AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size) }.getOrNull()
                ?: return@withContext
            val buffer = ShortArray(rate / 5)
            try {
                recorder.startRecording()
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    decibels(buffer, read)?.let { latest = it.toInt() }
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
            }
        }
    }
    LaunchedEffect(latest) { onLevel(latest) }
}

private const val SAMPLE_RATE = 44_100
private const val MAX_DB = 110f
private const val LOUD_DB = 85
