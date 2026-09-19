package dev.qtremors.osyster.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.ui.util.OsysterHapticUtil

val LocalReducedMotionEnabled = staticCompositionLocalOf { false }

/**
 * Centralized motion tokens defining standard physics spring specs for the design system.
 */
object MotionTokens {
    // Touch feedback spring: tactile, responsive, subtle bounce
    val TouchBounceSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMediumLow
    )

    // Corner and shape morphing: critically damped, no overshoot
    val MorphSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val MorphDpSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Screen navigation enter: gentle low bouncy slide
    val NavigationSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Screen navigation exit with parallax: critically damped to avoid reverse overshoot
    val NavigationParallaxExitSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Dynamic content / sheet expand / collapse: low bouncy
    val ContentSpring = spring<IntSize>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Telemetry gauge & arc smoothing: critically damped, steady tracking
    val GaugeSmoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Weight morphing for weighted buttons & tab rows
    val WeightSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Rapid feedback shake for disabled actions
    val DisabledShakeSpring = spring<Float>(
        stiffness = 10000f
    )
}

/**
 * Adds an expressive scale compression to any component when touched/pressed, without consuming gestures.
 */
fun Modifier.pressBounce(
    targetScale: Float = 0.96f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotionEnabled.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) targetScale else 1f,
        animationSpec = MotionTokens.TouchBounceSpring,
        label = "pressBounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * Adds an expressive scale compression to any component when pressed, responding to its MutableInteractionSource.
 */
fun Modifier.pressBounce(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    targetScale: Float = 0.96f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotionEnabled.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) targetScale else 1f,
        animationSpec = MotionTokens.TouchBounceSpring,
        label = "pressBounceScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Universal tactile clickable modifier for cards, list items, chips, and surfaces.
 * Compresses slightly on press and springs back naturally, accompanied by virtual key haptics.
 */
fun Modifier.expressiveClickable(
    shape: Shape? = null,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    hapticFeedback: Boolean = true,
    pressScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotionEnabled.current
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) pressScale else 1f,
        animationSpec = MotionTokens.TouchBounceSpring,
        label = "expressiveClickScale"
    )

    val baseModifier = if (shape != null) this.clip(shape) else this
    baseModifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            role = role,
            onClick = {
                if (hapticFeedback) {
                    OsysterHapticUtil.performVirtualKey(view, true)
                }
                onClick()
            }
        )
}

fun Modifier.bounceClickable(
    shape: Shape? = null,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotionEnabled.current
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.96f else 1f,
        animationSpec = MotionTokens.TouchBounceSpring,
        label = "bounceClickScale"
    )

    val baseModifier = if (shape != null) this.clip(shape) else this
    baseModifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = {
                try {
                    OsysterHapticUtil.performVirtualKey(view, true)
                } catch (_: Exception) {
                }
                onClick()
            }
        )
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceCombinedClickable(
    shape: Shape? = null,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotionEnabled.current
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.96f else 1f,
        animationSpec = MotionTokens.TouchBounceSpring,
        label = "bounceCombinedClickScale"
    )

    val baseModifier = if (shape != null) this.clip(shape) else this
    baseModifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick?.let { action ->
                {
                    try {
                        OsysterHapticUtil.performReject(view, true)
                    } catch (_: Exception) {
                    }
                    action()
                }
            },
            onClick = {
                try {
                    OsysterHapticUtil.performVirtualKey(view, true)
                } catch (_: Exception) {
                }
                onClick()
            }
        )
}
