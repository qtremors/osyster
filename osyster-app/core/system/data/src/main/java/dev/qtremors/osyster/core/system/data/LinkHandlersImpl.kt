package dev.qtremors.osyster.core.system.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import dev.qtremors.osyster.core.system.domain.LinkHandler
import dev.qtremors.osyster.core.system.domain.LinkHandlers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/** Finds the apps that can open a link, for the Link actions chooser. */
@Singleton
internal class LinkHandlersImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) : LinkHandlers {
    private val pm = context.packageManager

    override suspend fun handlersFor(url: String): List<LinkHandler> = withContext(dispatchers.io) {
        val uri = Uri.parse(url)
        val browsers = resolve(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))).map { it.activityInfo.packageName }.toSet()
        resolve(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
            .filter { it.activityInfo.packageName != context.packageName }
            .map { LinkHandler(it.activityInfo.packageName, it.activityInfo.name, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName to it.className }
            // Apps made for this link (a video app for a video link) come before general browsers.
            .sortedWith(compareBy<LinkHandler> { it.packageName in browsers }.thenBy { it.label.lowercase() })
    }

    override fun open(url: String, handler: LinkHandler): Boolean = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setComponent(ComponentName(handler.packageName, handler.className))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)

    override fun isDefaultForLinks(): Boolean {
        val default = pm.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_DEFAULT_ONLY)
        return default?.activityInfo?.packageName == context.packageName
    }

    private fun resolve(intent: Intent) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
}
