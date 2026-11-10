package dev.qtremors.osyster.feature.barcode

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.ui.theme.OsysterTheme
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "Scan with Osyster" in the share sheet: reads the code in a shared image over the app it came
 * from. The result is saved to the history like any scan. If no code reads, the crop frame opens.
 */
@AndroidEntryPoint
class ShareScanActivity : ComponentActivity() {
    @Inject lateinit var themeSettings: ThemeSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.let { IntentCompat.getParcelableExtra(it, Intent.EXTRA_STREAM, Uri::class.java) }
        if (uri == null) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val options by themeSettings.options.collectAsStateWithLifecycle(initialValue = null)
            val loaded = options ?: return@setContent
            OsysterTheme(options = loaded) { ShareScan(uri, onDone = ::finish) }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ShareScan(uri: Uri, onDone: () -> Unit) {
        val viewModel: ScannerViewModel = hiltViewModel()
        val shown by viewModel.shown.collectAsStateWithLifecycle()
        val sites by viewModel.sites.collectAsStateWithLifecycle()
        var cropping by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(uri) {
            val bitmap = withContext(Dispatchers.IO) { CodeReader.load(this@ShareScanActivity, uri) }
            if (bitmap == null) {
                Toast.makeText(this@ShareScanActivity, R.string.scan_picture_unreadable, Toast.LENGTH_SHORT).show()
                onDone()
                return@LaunchedEffect
            }
            val code = withContext(Dispatchers.Default) { CodeReader.read(bitmap) }
            if (code != null) viewModel.onRead(code) else cropping = bitmap
        }
        shown?.let { ScanResultSheet(code = it, sites = sites, onDismiss = onDone) }
        cropping?.let { bitmap ->
            CropSheet(bitmap = bitmap, onFound = { code ->
                cropping = null
                viewModel.onRead(code)
            }, onDismiss = onDone)
        }
    }
}
