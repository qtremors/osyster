package dev.qtremors.osyster.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import dev.qtremors.osyster.core.ui.theme.spacing

/**
 * The frame of every screen: a large title that collapses on scroll, the tonal page background,
 * and content padding that keeps the last row clear of the floating toolbar.
 *
 * [content] receives padding that already includes the side gutter and toolbar clearance.
 */
@Composable
fun OsysterScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val spacing = MaterialTheme.spacing
    val layoutDirection = LocalLayoutDirection.current
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.surfaceContainer,
        snackbarHost = snackbarHost,
        topBar = {
            LargeTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surfaceContainer,
                    scrolledContainerColor = colors.surfaceContainer
                )
            )
        }
    ) { inner ->
        content(
            PaddingValues(
                start = inner.calculateStartPadding(layoutDirection) + spacing.screenGutter,
                end = inner.calculateEndPadding(layoutDirection) + spacing.screenGutter,
                top = inner.calculateTopPadding(),
                bottom = inner.calculateBottomPadding() + spacing.toolbarClearance
            )
        )
    }
}
