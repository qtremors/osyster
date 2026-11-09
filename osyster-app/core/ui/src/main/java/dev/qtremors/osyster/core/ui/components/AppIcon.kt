package dev.qtremors.osyster.core.ui.components

import android.content.Context
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An installed app's launcher icon, loaded off the main thread and cached. Apps without an icon
 * (system processes, removed apps) show a neutral Android glyph instead.
 */
@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val current = rememberAppIconBitmap(packageName)
    if (current != null) {
        Image(bitmap = current, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(
            modifier = modifier.size(size).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The app's icon as a bitmap, for callers that draw it themselves (for example grayed out). */
@Composable
fun rememberAppIconBitmap(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = AppIconCache.peek(packageName), packageName) {
        if (value == null) value = withContext(Dispatchers.IO) { AppIconCache.load(context, packageName) }
    }
    return icon
}

private object AppIconCache {
    private const val ICON_PX = 128
    private val cache = LruCache<String, ImageBitmap>(200)
    private val missing = mutableSetOf<String>()

    fun peek(packageName: String): ImageBitmap? = cache.get(packageName)

    fun load(context: Context, packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        if (packageName in missing) return null
        val bitmap = runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
        }.getOrNull()
        if (bitmap == null) synchronized(missing) { missing += packageName } else cache.put(packageName, bitmap)
        return bitmap
    }
}
