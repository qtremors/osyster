package dev.qtremors.osyster.feature.barcode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ScannerScreen(viewModel: ScannerViewModel, onHistory: () -> Unit, onAppSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val shown by viewModel.shown.collectAsStateWithLifecycle()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var cropping by remember { mutableStateOf<Bitmap?>(null) }
    var unreadable by remember { mutableStateOf(false) }

    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var granted by remember { mutableStateOf(cameraGranted(context)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    LaunchedEffect(Unit) { if (hasCamera && !granted && !asked) askCamera.launch(Manifest.permission.CAMERA) }

    // A read from any source: beep and vibrate as set, then show it (and save it to the history).
    val analyzer = remember {
        CodeAnalyzer { code ->
            scope.launch(Dispatchers.Main) {
                if (viewModel.settings.value.beep) beep()
                if (viewModel.settings.value.vibrate) haptics.confirm()
                viewModel.onRead(code)
            }
        }
    }
    LaunchedEffect(shown) { if (shown == null) analyzer.resume() }

    val pickPicture = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { CodeReader.load(context, uri) }
            if (bitmap == null) {
                unreadable = true
                return@launch
            }
            val code = withContext(Dispatchers.Default) { CodeReader.read(bitmap) }
            if (code != null) viewModel.onRead(code) else cropping = bitmap
        }
    }

    OsysterScaffold(
        title = stringResource(Tool.BARCODE.label.title),
        actions = {
            IconButton(shapes = IconButtonDefaults.shapes(), onClick = { settingsOpen = true }) {
                Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.scan_settings))
            }
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)
        ) {
            when {
                !hasCamera -> EmptyState(Icons.Rounded.NoPhotography, stringResource(R.string.scan_no_camera_title), stringResource(R.string.scan_no_camera_message))
                !granted -> EmptyState(
                    icon = Icons.Rounded.PhotoCamera,
                    title = stringResource(R.string.scan_camera_title),
                    message = stringResource(R.string.scan_camera_message),
                    actionLabel = stringResource(if (asked) R.string.scan_camera_settings else R.string.scan_camera_allow),
                    onAction = { if (asked) onAppSettings() else askCamera.launch(Manifest.permission.CAMERA) }
                )
                else -> CameraPanel(analyzer)
            }
            if (unreadable) {
                Text(stringResource(R.string.scan_picture_unreadable), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        unreadable = false
                        pickPicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Text(stringResource(R.string.scan_picture), modifier = Modifier.padding(start = 8.dp))
                }
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = onHistory, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Rounded.History, contentDescription = null)
                    Text(stringResource(R.string.scan_history), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    shown?.let { ScanResultSheet(code = it, sites = sites, onDismiss = viewModel::dismiss) }
    cropping?.let { bitmap ->
        CropSheet(bitmap = bitmap, onFound = { code ->
            cropping = null
            viewModel.onRead(code)
        }, onDismiss = { cropping = null })
    }
    if (settingsOpen) {
        ScannerSettingsSheet(
            settings = settings,
            sites = sites,
            onChange = viewModel::update,
            onAddSite = viewModel::addSite,
            onDeleteSite = viewModel::deleteSite,
            onDismiss = { settingsOpen = false }
        )
    }
}

/** The live camera with the scan frame, torch, and zoom. */
@Composable
private fun CameraPanel(analyzer: CodeAnalyzer) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var request by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by rememberSaveable { mutableStateOf(false) }
    var zoom by rememberSaveable { mutableFloatStateOf(0f) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    LaunchedEffect(owner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().also { it.setSurfaceProvider { surface -> request = surface } }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, analyzer) }
        // Unbind only this panel's own use cases: when the screen is recreated, the old effect's
        // cleanup can run after the new one has bound, and unbindAll() would shut the new preview.
        camera = runCatching { provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) }.getOrNull()
        try {
            awaitCancellation()
        } finally {
            provider.unbind(preview, analysis)
            camera = null
        }
    }
    LaunchedEffect(camera, torch) { camera?.cameraControl?.enableTorch(torch) }
    LaunchedEffect(camera, zoom) { camera?.cameraControl?.setLinearZoom(zoom) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(32.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, change, _ -> zoom = (zoom + (change - 1f) * PINCH_SPEED).coerceIn(0f, 1f) }
                }
        ) {
            request?.let { CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize()) }
            ScanFrame()
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            ) {
                Text(stringResource(R.string.scan_hint), color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                FilledIconToggleButton(checked = torch, onCheckedChange = { torch = it }, shapes = IconButtonDefaults.toggleableShapes(), modifier = Modifier.size(56.dp)) {
                    Icon(
                        if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        contentDescription = stringResource(if (torch) R.string.scan_torch_off else R.string.scan_torch_on)
                    )
                }
            }
            Icon(Icons.Rounded.ZoomIn, contentDescription = stringResource(R.string.scan_zoom), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = zoom, onValueChange = { zoom = it }, modifier = Modifier.weight(1f))
        }
    }
}

/** Dims everything but the square the reader scans, and outlines it. */
@Composable
private fun ScanFrame() {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val side = minOf(size.width, size.height) * CodeReader.FRAME_SHARE
        val topLeft = Offset((size.width - side) / 2, (size.height - side) / 2)
        val radius = CornerRadius(28.dp.toPx())
        val hole = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(topLeft, Size(side, side)), radius)) }
        clipPath(hole, clipOp = ClipOp.Difference) { drawRect(Color.Black.copy(alpha = 0.35f)) }
        drawRoundRect(accent, topLeft, Size(side, side), radius, style = Stroke(4.dp.toPx()))
    }
}

private fun cameraGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** A short beep on the notification stream, so it follows the phone's notification volume. */
private fun beep() {
    runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, BEEP_VOLUME)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_MILLIS)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, BEEP_MILLIS + 100L)
    }
}

private const val PINCH_SPEED = 0.6f
private const val BEEP_VOLUME = 70
private const val BEEP_MILLIS = 120
