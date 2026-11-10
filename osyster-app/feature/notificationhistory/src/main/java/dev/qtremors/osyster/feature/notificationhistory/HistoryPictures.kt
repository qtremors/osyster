package dev.qtremors.osyster.feature.notificationhistory

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A saved notification picture, decoded off the main thread at about [maxPx] on its long side
 * and kept in a small cache, so scrolling the list does not decode the same file twice.
 */
@Composable
internal fun rememberPicture(path: String?, maxPx: Int): ImageBitmap? {
    val key = path?.let { "$it@$maxPx" }
    val picture by produceState(initialValue = key?.let(cache::get), key) {
        if (value == null && path != null && key != null) {
            value = withContext(Dispatchers.IO) { decode(path, maxPx)?.also { cache.put(key, it) } }
        }
    }
    return picture
}

private fun decode(path: String, maxPx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()

private val cache = LruCache<String, ImageBitmap>(48)
