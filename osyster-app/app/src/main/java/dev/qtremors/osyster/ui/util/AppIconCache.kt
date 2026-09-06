package dev.qtremors.osyster.ui.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// =========================================================================
// Section Comment: Bounded In-Memory App Icon Cache
// =========================================================================

object AppIconCache {
    private const val MAX_ENTRIES = 200
    private val cache = LruCache<String, ImageBitmap>(MAX_ENTRIES)

    fun get(packageName: String): ImageBitmap? = cache.get(packageName)

    fun put(packageName: String, bitmap: ImageBitmap) {
        cache.put(packageName, bitmap)
    }

    fun contains(packageName: String): Boolean = cache.get(packageName) != null

    fun size(): Int = cache.size()

    fun clear() {
        cache.evictAll()
    }
}

fun Drawable.toSafeBitmap(): Bitmap {
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    return toBitmap(width.coerceIn(48, 144), height.coerceIn(48, 144))
}

@Composable
fun rememberAsyncAppIcon(
    packageName: String,
    fallbackDrawable: Drawable? = null
): State<ImageBitmap?> {
    val context = LocalContext.current.applicationContext
    val initialCached = remember(packageName) { AppIconCache.get(packageName) }
    val iconState = remember(packageName) { mutableStateOf(initialCached) }

    LaunchedEffect(packageName, fallbackDrawable) {
        if (iconState.value != null) return@LaunchedEffect

        val cached = AppIconCache.get(packageName)
        if (cached != null) {
            iconState.value = cached
            return@LaunchedEffect
        }

        val resolved = withContext(Dispatchers.IO) {
            AppIconCache.get(packageName) ?: run {
                val drawable = fallbackDrawable ?: runCatching {
                    context.packageManager.getApplicationIcon(packageName)
                }.getOrNull()

                drawable?.let {
                    runCatching {
                        it.toSafeBitmap().asImageBitmap()
                    }.getOrNull()
                }?.also {
                    AppIconCache.put(packageName, it)
                }
            }
        }
        iconState.value = resolved
    }

    return iconState
}
