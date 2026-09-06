package dev.qtremors.osyster.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.qtremors.osyster.R
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
    actionButton: (@Composable () -> Unit)? = null,
    customContent: (@Composable RowScope.() -> Unit)? = null,
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
        if (customContent != null) {
            customContent()
        } else if (onBackClick != null) {
            IconButton(
                onClick = {
                    OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                    onBackClick()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterVertically),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!title.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                val maxTitleWidth = if (floatingActionButton != null) {
                    (screenWidth - 190).coerceAtLeast(80).dp
                } else {
                    (screenWidth - 140).coerceAtLeast(100).dp
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .widthIn(max = maxTitleWidth)
                        .padding(start = 2.dp, end = if (actionButton == null) 14.dp else 4.dp)
                        .basicMarquee()
                )
            }

            if (actionButton != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                    actionButton()
                }
            }
        } else {
            Row(
                modifier = Modifier.selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index

                    val itemWidth by animateDpAsState(
                        targetValue = if (expanded || isSelected) 48.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "dock_item_width_$index"
                    )

                    val labelWidth by animateDpAsState(
                        targetValue = if (isSelected && !shouldHideLabel) 70.dp else 0.dp,
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
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .width(itemWidth + labelWidth)
                                .height(48.dp)
                                .selectable(
                                    selected = isSelected,
                                    role = Role.Tab,
                                    onClick = {
                                        OsysterHapticUtil.performVirtualKey(view, hapticEnabled)
                                        item.onClick()
                                    }
                                )
                                .semantics {
                                    contentDescription = item.label
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
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
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp, top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (floatingActionButton != null) {
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
