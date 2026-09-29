package dev.qtremors.osyster.feature.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A project Osyster ships with, and the bundled license text that covers it. */
private data class Credit(@param:StringRes val name: Int, @param:StringRes val detail: Int, val licenseAsset: String)

private val credits = listOf(
    Credit(R.string.credit_osyster, R.string.credit_osyster_detail, "licenses/gpl-3.0.txt"),
    Credit(R.string.credit_essentials, R.string.credit_essentials_detail, "licenses/mit-essentials.txt"),
    Credit(R.string.credit_metro, R.string.credit_metro_detail, "licenses/gpl-3.0.txt"),
    Credit(R.string.credit_androidx, R.string.credit_androidx_detail, "licenses/apache-2.0.txt"),
    Credit(R.string.credit_kotlin, R.string.credit_kotlin_detail, "licenses/apache-2.0.txt"),
    Credit(R.string.credit_hilt, R.string.credit_hilt_detail, "licenses/apache-2.0.txt"),
    Credit(R.string.credit_icons, R.string.credit_icons_detail, "licenses/apache-2.0.txt"),
    Credit(R.string.credit_materialkolor, R.string.credit_materialkolor_detail, "licenses/mit-materialkolor.txt"),
    Credit(R.string.credit_shizuku, R.string.credit_shizuku_detail, "licenses/mit-shizuku.txt"),
    Credit(R.string.credit_roboto_flex, R.string.credit_roboto_flex_detail, "licenses/ofl-roboto-flex.txt")
)

/** Every project Osyster builds on. Tap one to read its full license, selectable for copying. */
@Composable
internal fun LicensesScreen() {
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    OsysterScaffold(title = stringResource(R.string.about_licenses)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "header") { SectionHeader(stringResource(R.string.licenses_intro)) }
            items(credits, key = { it.name }) { credit ->
                Column(modifier = Modifier.padding(bottom = 2.dp)) {
                    ConnectedGroup {
                        ConnectedRow(
                            title = stringResource(credit.name),
                            supporting = stringResource(credit.detail),
                            onClick = { expanded = if (expanded == credit.name) null else credit.name }
                        )
                    }
                    AnimatedVisibility(visible = expanded == credit.name) { LicenseText(credit.licenseAsset) }
                }
            }
        }
    }
}

@Composable
private fun LicenseText(asset: String) {
    val context = LocalContext.current
    val text by produceState(initialValue = "", asset) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(asset).bufferedReader().use { reflow(it.readText()) } }.getOrDefault("")
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.large, modifier = Modifier.padding(top = 2.dp)) {
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * License files are hard-wrapped at about 80 columns, which breaks sentences mid-line on a phone.
 * Lines inside a paragraph are joined; blank lines, list items, and indented lines keep their own line.
 */
internal fun reflow(text: String): String {
    val listItem = Regex("""^(\d+\.|[a-z]\)|\([a-z0-9]+\)|[-*•])\s""")
    val paragraphs = text.replace("\r\n", "\n").split(Regex("""\n\s*\n"""))
    return paragraphs.joinToString("\n\n") { paragraph ->
        val out = StringBuilder()
        paragraph.lines().forEach { raw ->
            if (raw.isBlank()) return@forEach
            val ownLine = raw.startsWith(" ") || raw.startsWith("\t") || listItem.containsMatchIn(raw.trimStart())
            if (out.isNotEmpty()) out.append(if (ownLine) '\n' else ' ')
            out.append(raw.trim())
        }
        out.toString()
    }.trim()
}

