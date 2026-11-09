package dev.qtremors.osyster.shell

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import dev.qtremors.osyster.core.navigation.HomeRoute
import dev.qtremors.osyster.core.navigation.SettingsRoute
import dev.qtremors.osyster.core.navigation.ToolsRoute

/*
 * Page motion. Springs, not fixed-length tweens: a spring picks up from wherever the back gesture
 * left the page, so releasing mid-swipe settles naturally instead of replaying a slow animation.
 * Tabs crossfade; detail pages slide in from the side and slide back out the same way.
 */

private fun <T> pageSpring(): SpringSpec<T> = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

private val offsetSpring: SpringSpec<IntOffset> = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

internal fun NavDestination?.isTopLevelPage(): Boolean =
    this == null || hasRoute<HomeRoute>() || hasRoute<ToolsRoute>() || hasRoute<SettingsRoute>()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs(): Boolean =
    initialState.destination.isTopLevelPage() && targetState.destination.isTopLevelPage()

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.shellEnter(reduceMotion: Boolean): EnterTransition = when {
    reduceMotion -> fadeIn(tween(0))
    betweenTabs() -> fadeIn(pageSpring())
    else -> slideInHorizontally(offsetSpring) { it } + fadeIn(pageSpring()) + scaleIn(pageSpring(), initialScale = 0.94f)
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.shellExit(reduceMotion: Boolean): ExitTransition = when {
    reduceMotion -> fadeOut(tween(0))
    betweenTabs() -> fadeOut(pageSpring())
    else -> slideOutHorizontally(offsetSpring) { -it / 3 } + fadeOut(pageSpring()) + scaleOut(pageSpring(), targetScale = 1.04f)
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.shellPopEnter(reduceMotion: Boolean): EnterTransition = when {
    reduceMotion -> fadeIn(tween(0))
    betweenTabs() -> fadeIn(pageSpring())
    else -> slideInHorizontally(offsetSpring) { -it / 3 } + fadeIn(pageSpring()) + scaleIn(pageSpring(), initialScale = 1.04f)
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.shellPopExit(reduceMotion: Boolean): ExitTransition = when {
    reduceMotion -> fadeOut(tween(0))
    betweenTabs() -> fadeOut(pageSpring())
    else -> slideOutHorizontally(offsetSpring) { it } + fadeOut(pageSpring()) + scaleOut(pageSpring(), targetScale = 0.94f)
}
