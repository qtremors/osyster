package dev.qtremors.osyster.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.R
import dev.qtremors.osyster.core.ui.theme.LocalReduceMotion

@Immutable
data class TabItem(val icon: ImageVector, val label: String)

/**
 * The floating pill at the bottom of every screen. On a top-level screen it shows the tabs; on a
 * detail page it turns into a Back button with the page title, so Back is always in thumb reach.
 */
@Composable
fun FloatingTabBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backTitle: String? = null,
    onBack: (() -> Unit)? = null
) {
    val reduceMotion = LocalReduceMotion.current
    val colors = MaterialTheme.colorScheme
    val currentOnBack by rememberUpdatedState(onBack)
    // The mode is the animation's target, so the outgoing content keeps the title it had. Reading
    // the live title instead made the leaving "Back + title" collapse to a lone circle mid-animation.
    val mode = if (onBack != null) BarMode.Back(backTitle) else BarMode.Tabs
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 12.dp),
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContainerColor = colors.primary,
            toolbarContentColor = colors.onPrimary
        )
    ) {
        AnimatedContent(
            targetState = mode,
            // Only switching between tabs and Back animates; a new title just updates in place.
            contentKey = { it is BarMode.Back },
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.9f)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessHigh)) using
                        SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow) }
                }
            },
            contentAlignment = Alignment.Center,
            label = "toolbarMode"
        ) { shown ->
            when (shown) {
                is BarMode.Back -> BackContent(title = shown.title, onBack = { currentOnBack?.invoke() })
                BarMode.Tabs -> TabsContent(tabs = tabs, selectedIndex = selectedIndex, onSelect = onSelect)
            }
        }
    }
}

/** What the bar shows. The title travels with the Back mode so it survives the exit animation. */
private sealed interface BarMode {
    data object Tabs : BarMode
    data class Back(val title: String?) : BarMode
}

@Composable
private fun TabsContent(tabs: List<TabItem>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val haptics = rememberAppHaptics()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val container by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
                label = "tabContainer"
            )
            val content by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                label = "tabContent"
            )
            val width by animateDpAsState(
                targetValue = if (selected) 64.dp else 48.dp,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                label = "tabWidth"
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 48.dp)
                    .clip(CircleShape)
                    .background(container)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = {
                            if (!selected) haptics.tap()
                            onSelect(index)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = tab.icon, contentDescription = tab.label, tint = content)
            }
        }
    }
}

@Composable
private fun BackContent(title: String?, onBack: () -> Unit) {
    val haptics = rememberAppHaptics()
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            onClick = {
                haptics.tap()
                onBack()
            },
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back)
            )
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .padding(horizontal = 16.dp)
                    .basicMarquee()
            )
        }
    }
}
