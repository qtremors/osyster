package dev.qtremors.osyster.feature.linkactions

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.system.domain.LinkHandlers
import dev.qtremors.osyster.core.ui.theme.OsysterTheme
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import javax.inject.Inject

/**
 * Receives web links when Osyster is the default app for them and shows the chooser over
 * whatever app the link came from. Nothing is loaded or logged; the link only goes where the user
 * taps.
 */
@AndroidEntryPoint
class LinkChooserActivity : ComponentActivity() {
    @Inject lateinit var links: LinkHandlers
    @Inject lateinit var themeSettings: ThemeSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val url = intent?.dataString
        if (url.isNullOrBlank()) {
            finish()
            return
        }
        setContent {
            val options by themeSettings.options.collectAsStateWithLifecycle(initialValue = null)
            val loaded = options ?: return@setContent
            OsysterTheme(options = loaded) {
                LinkChooserSheet(url = url, links = links, onShare = ::share, onDone = ::finish)
            }
        }
    }

    private fun share(url: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
