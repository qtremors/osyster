package dev.qtremors.osyster.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.ui.util.OsysterHapticUtil

data class OsysterDockItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OsysterDock(
    modifier: Modifier = Modifier,
    items: List<OsysterDockItem> = emptyList(),
    selectedIndex: Int = -1,
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    hapticEnabled: Boolean = true,
    expanded: Boolean = true
) {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val screenWidth = configuration.screenWidthDp

    val isLargeFont = fontScale > 1.25f
    val isCompactScreen = screenWidth < 380
    val shouldHideLabel = isLargeFont || (isCompactScreen && items.size > 2)

    val toolbarContent: @Composable RowScope.() -> Unit = {
        if (onBackClick != null) {
            IconButton(
                onClick = {
                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                    onBackClick()
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(22.dp)
                )
            }

            if (!title.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .height(44.dp)
                        .widthIn(min = 60.dp, max = 220.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier
                            .basicMarquee()
                            .weight(1f, fill = false)
                    )
                }
            }
        } else {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index

                val itemWidth by animateDpAsState(
                    targetValue = if (expanded || isSelected) 44.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "dock_item_width_$index"
                )

                val labelWidth by animateDpAsState(
                    targetValue = if (isSelected && !shouldHideLabel) 76.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "dock_label_width_$index"
                )

                val spacerWidth by animateDpAsState(
                    targetValue = if (index < items.size - 1) 6.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "dock_spacer_width_$index"
                )

                if (itemWidth > 0.dp || isSelected) {
                    IconButton(
                        onClick = {
                            OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                            item.onClick()
                        },
                        modifier = Modifier
                            .width(itemWidth + labelWidth)
                            .height(44.dp),
                        colors = if (isSelected) {
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp)
                            )

                            if (isSelected && !shouldHideLabel) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }

                    if (index < items.size - 1) {
                        Spacer(modifier = Modifier.width(spacerWidth))
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (floatingActionButton != null && onBackClick == null) {
            HorizontalFloatingToolbar(
                expanded = expanded,
                floatingActionButton = floatingActionButton,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                    toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    toolbarContentColor = MaterialTheme.colorScheme.onSurface
                ),
                content = toolbarContent
            )
        } else {
            HorizontalFloatingToolbar(
                expanded = expanded,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                    toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    toolbarContentColor = MaterialTheme.colorScheme.onSurface
                ),
                content = toolbarContent
            )
        }
    }
}
