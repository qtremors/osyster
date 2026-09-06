package dev.qtremors.osyster.ui.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal providing dynamic bottom content padding so that scrollable content
 * can scroll comfortably above the floating dock across varied navigation bars and insets.
 */
val LocalBottomContentPadding = compositionLocalOf<Dp> { 100.dp }
