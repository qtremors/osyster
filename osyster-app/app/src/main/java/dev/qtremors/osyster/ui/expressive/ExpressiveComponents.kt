@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.qtremors.osyster.ui.expressive

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =========================================================================
// Group Position & Connected Shapes
// =========================================================================

enum class GroupPosition {
    Top, Middle, Bottom, Single
}

fun expressiveGroupShape(
    position: GroupPosition,
    outerCorner: Dp = 24.dp,
    innerCorner: Dp = 8.dp
): RoundedCornerShape {
    return when (position) {
        GroupPosition.Top -> RoundedCornerShape(
            topStart = outerCorner,
            topEnd = outerCorner,
            bottomStart = innerCorner,
            bottomEnd = innerCorner
        )
        GroupPosition.Middle -> RoundedCornerShape(innerCorner)
        GroupPosition.Bottom -> RoundedCornerShape(
            topStart = innerCorner,
            topEnd = innerCorner,
            bottomStart = outerCorner,
            bottomEnd = outerCorner
        )
        GroupPosition.Single -> RoundedCornerShape(outerCorner)
    }
}

fun expressiveGroupShape(
    index: Int,
    total: Int,
    outerCorner: Dp = 24.dp,
    innerCorner: Dp = 8.dp
): RoundedCornerShape {
    val position = when {
        total <= 1 -> GroupPosition.Single
        index == 0 -> GroupPosition.Top
        index == total - 1 -> GroupPosition.Bottom
        else -> GroupPosition.Middle
    }
    return expressiveGroupShape(position, outerCorner, innerCorner)
}

// =========================================================================
// Digit Ticker (Rolling Numbers with AnimatedContent)
// =========================================================================

@Composable
fun DigitTicker(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = ""
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        text.forEachIndexed { index, char ->
            val key = "${prefix}_${text.length - index}"
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (targetState.isDigit() && initialState.isDigit()) {
                        if (targetState > initialState) {
                            (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                        } else {
                            (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
                        }
                    } else {
                        fadeIn() togetherWith fadeOut()
                    }
                },
                label = "DigitTicker_$key",
                contentAlignment = Alignment.BottomStart
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style.copy(
                        letterSpacing = (-1.5).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Bottom,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    color = color,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun TickerUnit(
    unit: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displaySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = unit,
        style = style.copy(
            letterSpacing = (-1).sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Bottom,
                trim = LineHeightStyle.Trim.Both
            )
        ),
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
    )
}

// =========================================================================
// Expressive Squishy Buttons & Controls
// =========================================================================

enum class ExpressiveButtonType {
    Filled, Tonal, Outlined, Text, Hold
}

enum class ExpressiveButtonSize {
    Small, Medium, Large
}

@Composable
fun OsysterExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ExpressiveButtonType = ExpressiveButtonType.Filled,
    size: ExpressiveButtonSize = ExpressiveButtonSize.Large,
    text: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    onHoldComplete: (() -> Unit)? = null,
    holdDuration: Long = 1500L,
    shape: Shape? = null,
    fillMaxWidth: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val holdProgress = remember { Animatable(0f) }

    LaunchedEffect(isPressed) {
        if (onHoldComplete != null || type == ExpressiveButtonType.Hold) {
            if (isPressed && enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                holdProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(holdDuration.toInt())
                )
                if (holdProgress.value >= 1f) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onHoldComplete?.invoke()
                    onClick()
                }
            } else {
                holdProgress.snapTo(0f)
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    val height = when (size) {
        ExpressiveButtonSize.Small -> 36.dp
        ExpressiveButtonSize.Medium -> 44.dp
        ExpressiveButtonSize.Large -> 54.dp
    }

    val restCorner = height / 2
    val pressedCorner = 12.dp

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) pressedCorner else restCorner,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_corner"
    )

    val buttonShape = shape ?: RoundedCornerShape(cornerRadius)

    val defaultBg = when (type) {
        ExpressiveButtonType.Filled -> containerColor ?: MaterialTheme.colorScheme.primary
        ExpressiveButtonType.Tonal -> containerColor ?: MaterialTheme.colorScheme.secondaryContainer
        ExpressiveButtonType.Outlined -> Color.Transparent
        ExpressiveButtonType.Text -> Color.Transparent
        ExpressiveButtonType.Hold -> containerColor ?: MaterialTheme.colorScheme.primaryContainer
    }

    val defaultFg = when (type) {
        ExpressiveButtonType.Filled -> contentColor ?: MaterialTheme.colorScheme.onPrimary
        ExpressiveButtonType.Tonal -> contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer
        ExpressiveButtonType.Outlined -> contentColor ?: MaterialTheme.colorScheme.primary
        ExpressiveButtonType.Text -> contentColor ?: MaterialTheme.colorScheme.primary
        ExpressiveButtonType.Hold -> contentColor ?: MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        onClick = {
            if (enabled && onHoldComplete == null && type != ExpressiveButtonType.Hold) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        enabled = enabled,
        shape = buttonShape,
        color = defaultBg,
        contentColor = defaultFg,
        border = if (type == ExpressiveButtonType.Outlined) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        interactionSource = interactionSource,
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (holdProgress.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(holdProgress.value)
                        .align(Alignment.CenterStart)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    if (!text.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                if (!text.isNullOrBlank()) {
                    Text(
                        text = text,
                        style = when (size) {
                            ExpressiveButtonSize.Small -> MaterialTheme.typography.labelMedium
                            ExpressiveButtonSize.Medium -> MaterialTheme.typography.labelLarge
                            ExpressiveButtonSize.Large -> MaterialTheme.typography.titleSmall
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.OsysterButtonWeighted(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    weight: Float = 1f,
    type: ExpressiveButtonType = ExpressiveButtonType.Filled,
    size: ExpressiveButtonSize = ExpressiveButtonSize.Medium,
    text: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    shape: Shape? = null
) {
    OsysterExpressiveButton(
        onClick = onClick,
        modifier = modifier.weight(weight),
        type = type,
        size = size,
        text = text,
        icon = icon,
        enabled = enabled,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = shape
    )
}

@Composable
fun OsysterGroupedButton(
    modifier: Modifier = Modifier,
    spacedBy: Dp = 4.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacedBy),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// =========================================================================
// Expressive Loading Indicator
// =========================================================================

@Composable
fun ExpressiveContainedLoadingIndicator(
    modifier: Modifier = Modifier
) {
    ContainedLoadingIndicator(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.onPrimary
    )
}
