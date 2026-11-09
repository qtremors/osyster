package dev.qtremors.osyster.feature.device

import androidx.compose.material3.ButtonDefaults
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

/** The test screens, in the order a tap steps through them. */
private enum class TesterMode { BLACK, WHITE, RED, GREEN, BLUE, GRAY, GRADIENT, TOUCH }

private val solidColors = mapOf(
    TesterMode.BLACK to Color.Black,
    TesterMode.WHITE to Color.White,
    TesterMode.RED to Color(0xFFFF0000),
    TesterMode.GREEN to Color(0xFF00FF00),
    TesterMode.BLUE to Color(0xFF0000FF),
    TesterMode.GRAY to Color(0xFF808080)
)

/**
 * Full-screen color fills for dead or stuck pixels, a gray ramp for banding, and a multi-touch
 * drawing test. System bars hide while it is open and come back when it closes.
 */
@Composable
internal fun ScreenTesterScreen() {
    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    var showHint by remember { mutableStateOf(true) }
    val mode = TesterMode.entries[modeIndex]
    ImmersiveWhileShown()
    LaunchedEffect(modeIndex) {
        showHint = true
        delay(HINT_MS)
        showHint = false
    }
    val next = { modeIndex = (modeIndex + 1) % TesterMode.entries.size }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (mode) {
            TesterMode.GRADIENT -> Box(
                Modifier.fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color.Black, Color.White)))
                    .pointerInput(Unit) { detectTapGestures { next() } }
            )
            TesterMode.TOUCH -> TouchTest(onNext = next)
            else -> Box(
                Modifier.fillMaxSize()
                    .background(solidColors.getValue(mode))
                    .pointerInput(Unit) { detectTapGestures { next() } }
            )
        }
        AnimatedVisibility(
            visible = showHint,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 24.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f)) {
                Text(
                    text = stringResource(if (mode == TesterMode.TOUCH) R.string.tester_hint_touch else R.string.tester_hint_tap),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/** Draws every finger's path; shows how many fingers are down. */
@Composable
private fun TouchTest(onNext: () -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var fingers by remember { mutableIntStateOf(0) }
    val ink = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val active = mutableMapOf<Long, Int>()
                    do {
                        val event = awaitPointerEvent()
                        fingers = event.changes.count { it.pressed }
                        event.changes.forEach { change ->
                            val id = change.id.value
                            if (change.pressed) {
                                val index = active.getOrPut(id) { strokes.add(emptyList()); strokes.lastIndex }
                                strokes[index] = strokes[index] + change.position
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    fingers = 0
                }
            }
        ) {
            strokes.forEach { points ->
                points.zipWithNext().forEach { (a, b) -> drawLine(ink, a, b, strokeWidth = 8f, cap = StrokeCap.Round) }
                points.lastOrNull()?.let { drawCircle(ink, radius = 18f, center = it, style = Stroke(width = 4f)) }
            }
        }
        Text(
            text = pluralStringResource(R.plurals.tester_fingers, fingers, fingers),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp)
        )
        FilledTonalButton(shapes = ButtonDefaults.shapes(), 
            onClick = {
                strokes.clear()
                onNext()
            },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp)
        ) { Text(stringResource(R.string.tester_next)) }
    }
}

/** Hides status and navigation bars and keeps the screen on, restoring both on exit. */
@Composable
private fun ImmersiveWhileShown() {
    val activity = LocalActivity.current
    val view = LocalView.current
    DisposableEffect(activity, view) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
    }
}

private const val HINT_MS = 2_500L
