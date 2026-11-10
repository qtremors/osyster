package dev.qtremors.osyster.feature.barcode

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Polyline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.osyster.core.barcode.domain.CodeField
import dev.qtremors.osyster.core.barcode.domain.CodeStyle
import dev.qtremors.osyster.core.barcode.domain.CreateKind
import dev.qtremors.osyster.core.barcode.domain.CreateProblem
import dev.qtremors.osyster.core.barcode.domain.WifiSecurity
import dev.qtremors.osyster.core.barcode.domain.parseContent
import dev.qtremors.osyster.core.barcode.domain.warningFor
import dev.qtremors.osyster.core.ui.components.MenuAction
import dev.qtremors.osyster.core.ui.components.OsysterMenu
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One creator form: the code drawn live above its fields, then style, share, and save. */
@Composable
internal fun CreateFormScreen(viewModel: CreateViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kind = viewModel.kind
    val values by viewModel.values.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val size by viewModel.size.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    var styling by rememberSaveable { mutableStateOf(false) }
    var saveMenu by remember { mutableStateOf(false) }

    val toast = { message: Int -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    val savePng = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val picture = viewModel.export()
            val saved = picture != null && withContext(Dispatchers.IO) { CodeImage.savePng(context, uri, picture) }
            toast(if (saved) R.string.create_saved else R.string.create_save_failed)
        }
    }
    val saveSvg = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val shown = preview
        val matrix = shown.matrix ?: return@rememberLauncherForActivityResult
        scope.launch {
            val saved = withContext(Dispatchers.IO) { CodeImage.saveSvg(context, uri, matrix, style, shown.text.orEmpty()) }
            toast(if (saved) R.string.create_saved else R.string.create_save_failed)
        }
    }
    // Named after what the code holds as the result sheet shows it, so a Wi-Fi password never
    // ends up in a file name.
    val fileName = preview.text?.let { text -> CodeImage.fileName(kind.name.lowercase().replace('_', '-'), parseContent(text, kind.format).headline(context)) }
        ?: kind.name.lowercase()

    OsysterScaffold(title = kind.title()) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)
        ) {
            PreviewCard(kind, preview, style)
            kind.fields.filter { field -> shows(field, values) }.forEach { field ->
                val problem = preview.problem?.takeIf { it.field == field && it !is CreateProblem.Missing }
                CreateField(field, values[field].orEmpty(), values, problem?.message(), viewModel::set)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { styling = true }, modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Rounded.Palette, contentDescription = null)
                    Text(stringResource(R.string.create_style), modifier = Modifier.padding(start = 8.dp))
                }
                FilledTonalButton(
                    shapes = ButtonDefaults.shapes(),
                    enabled = preview.matrix != null,
                    onClick = {
                        scope.launch {
                            val picture = viewModel.export()
                            val share = picture?.let { withContext(Dispatchers.IO) { CodeImage.share(context, it, fileName) } }
                            if (share == null) toast(R.string.create_share_failed) else context.startActivity(share)
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Text(stringResource(R.string.create_share), modifier = Modifier.padding(start = 8.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    Button(shapes = ButtonDefaults.shapes(), enabled = preview.matrix != null, onClick = { saveMenu = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Text(stringResource(R.string.create_save), modifier = Modifier.padding(start = 8.dp))
                    }
                    OsysterMenu(
                        expanded = saveMenu,
                        onDismissRequest = { saveMenu = false },
                        groups = listOf(
                            listOf(
                                MenuAction(stringResource(R.string.create_save_png), Icons.Rounded.Image, supporting = stringResource(R.string.create_save_png_detail)) { savePng.launch("$fileName.png") },
                                MenuAction(stringResource(R.string.create_save_svg), Icons.Rounded.Polyline, supporting = stringResource(R.string.create_save_svg_detail)) { saveSvg.launch("$fileName.svg") }
                            )
                        )
                    )
                }
            }
        }
    }

    if (styling) {
        CreateStyleSheet(kind = kind, style = style, size = size, onStyle = viewModel::style, onSize = viewModel::size, onDismiss = { styling = false })
    }
}

/** A Wi-Fi network with no security has no password to ask for. */
private fun shows(field: CodeField, values: Map<CodeField, String>): Boolean =
    !(field == CodeField.PASSWORD && values[CodeField.SECURITY] == WifiSecurity.NONE.name)

/**
 * The code as it will be saved, on a checkered backdrop when the background is see-through.
 * With nothing to draw yet it says what is missing; style problems show under the code.
 */
@Composable
private fun PreviewCard(kind: CreateKind, preview: CodePreview, style: CodeStyle) {
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            val image = preview.image
            if (image != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .aspectRatio(image.width.toFloat() / image.height, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    if (style.background == CodeStyle.TRANSPARENT) Checkers()
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = stringResource(R.string.create_preview),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                warningFor(style)?.let { warning ->
                    Text(stringResource(warning.message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            } else {
                Icon(kind.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(72.dp).alpha(0.5f))
                val problem = preview.problem
                // Field problems show under their field; only the rest are repeated here.
                val text = when {
                    problem == null -> null
                    problem is CreateProblem.Missing || problem.field == null -> problem.message()
                    else -> null
                }
                text?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (problem == CreateProblem.TooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
