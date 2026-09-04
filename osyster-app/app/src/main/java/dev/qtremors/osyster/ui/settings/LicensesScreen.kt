@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.qtremors.osyster.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.R
import dev.qtremors.osyster.ui.theme.expressiveSegmentedShapes

private data class OpenSourceLibrary(
    val name: String,
    val license: String,
    val url: String
)

private val libraries = listOf(
    OpenSourceLibrary("AndroidX Core KTX", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/core"),
    OpenSourceLibrary("AndroidX AppCompat", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/appcompat"),
    OpenSourceLibrary("AndroidX Activity Compose", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/activity"),
    OpenSourceLibrary("AndroidX Lifecycle Runtime KTX", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
    OpenSourceLibrary("AndroidX Lifecycle ViewModel Compose", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
    OpenSourceLibrary("AndroidX Navigation Compose", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/navigation"),
    OpenSourceLibrary("AndroidX Graphics Shapes", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/graphics"),
    OpenSourceLibrary("Jetpack Compose UI", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
    OpenSourceLibrary("Jetpack Compose Material 3 Expressive", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
    OpenSourceLibrary("Jetpack Compose Material 3 Adaptive", "Apache 2.0", "https://developer.android.com/develop/ui/compose/layouts/adaptive"),
    OpenSourceLibrary("Jetpack Compose Material Icons Extended", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
    OpenSourceLibrary("Kotlin Coroutines Android", "Apache 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    OpenSourceLibrary("Kotlinx Serialization JSON", "Apache 2.0", "https://github.com/Kotlin/kotlinx.serialization"),
    OpenSourceLibrary("Kotlinx Immutable Collections", "Apache 2.0", "https://github.com/Kotlin/kotlinx.collections.immutable"),
    OpenSourceLibrary("MaterialKolor", "MIT", "https://github.com/jordond/MaterialKolor")
)

@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.licenses_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Notice Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.licenses_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Credits & Acknowledgments
            item {
                SettingsSection(title = stringResource(R.string.section_credits)) {
                    // Tremors
                    SegmentedListItem(
                        onClick = { uriHandler.openUri("https://github.com/qtremors") },
                        shapes = expressiveSegmentedShapes(index = 0, count = 3),
                        content = {
                            Text(
                                text = stringResource(R.string.credit_author_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = { Text(stringResource(R.string.credit_author_desc)) },
                        leadingContent = {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingContent = {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.height(IntrinsicSize.Min)
                    )

                    // AOSP
                    SegmentedListItem(
                        onClick = { uriHandler.openUri("https://source.android.com") },
                        shapes = expressiveSegmentedShapes(index = 1, count = 3),
                        content = {
                            Text(
                                text = stringResource(R.string.credit_android_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = { Text(stringResource(R.string.credit_android_desc)) },
                        leadingContent = {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingContent = {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.height(IntrinsicSize.Min)
                    )

                    // Kotlin
                    SegmentedListItem(
                        onClick = { uriHandler.openUri("https://kotlinlang.org") },
                        shapes = expressiveSegmentedShapes(index = 2, count = 3),
                        content = {
                            Text(
                                text = stringResource(R.string.credit_kotlin_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = { Text(stringResource(R.string.credit_kotlin_desc)) },
                        leadingContent = {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingContent = {
                            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.height(IntrinsicSize.Min)
                    )
                }
            }

            // Open Source Libraries
            item {
                SettingsSection(title = stringResource(R.string.section_libraries)) {
                    libraries.forEachIndexed { index, lib ->
                        SegmentedListItem(
                            onClick = { uriHandler.openUri(lib.url) },
                            shapes = expressiveSegmentedShapes(index = index, count = libraries.size),
                            content = {
                                Text(
                                    text = lib.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = { Text(lib.license) },
                            trailingContent = {
                                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.height(IntrinsicSize.Min)
                        )
                    }
                }
            }
        }
    }
}
