package dev.qtremors.osyster.feature.musicwidgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.ui.components.AppIcon
import dev.qtremors.osyster.core.ui.components.ChoiceRow
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedItem
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.EmptyState
import dev.qtremors.osyster.core.ui.components.ExpressiveSwitch
import dev.qtremors.osyster.core.ui.components.LoadingState
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import dev.qtremors.osyster.core.ui.components.rememberAppHaptics
import dev.qtremors.osyster.core.ui.theme.OsysterTheme
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Which apps the music widgets follow: any app playing media, or only the ones picked here.
 * Opened from the Widgets page and from a music widget's setup screen; changes apply at once.
 */
@AndroidEntryPoint
class MusicAppsActivity : ComponentActivity() {
    @Inject lateinit var themeSettings: ThemeSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val options by themeSettings.options.collectAsStateWithLifecycle(initialValue = null)
            val loaded = options ?: return@setContent
            OsysterTheme(options = loaded) { MusicAppsScreen() }
        }
    }
}

@Composable
private fun MusicAppsScreen() {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    var onlyChosen by remember { mutableStateOf(MusicApps.onlyChosen(context)) }
    var chosen by remember { mutableStateOf(MusicApps.chosen(context)) }
    val apps by produceState<List<Pair<String, String>>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { MusicApps.candidates(context) }
    }

    OsysterScaffold(title = stringResource(R.string.music_apps_title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "mode") {
                ConnectedGroup {
                    ChoiceRow(
                        title = stringResource(R.string.music_apps_follow),
                        supporting = stringResource(if (onlyChosen) R.string.music_apps_follow_chosen_detail else R.string.music_apps_follow_any_detail),
                        options = listOf(false, true),
                        selected = onlyChosen,
                        label = { stringResource(if (it) R.string.music_apps_follow_chosen else R.string.music_apps_follow_any) },
                        onSelect = {
                            onlyChosen = it
                            MusicApps.setOnlyChosen(context, it)
                        }
                    )
                }
            }
            if (onlyChosen) {
                val list = apps
                when {
                    list == null -> item(key = "loading") { LoadingState() }
                    list.isEmpty() -> item(key = "empty") {
                        EmptyState(Icons.Rounded.LibraryMusic, stringResource(R.string.music_apps_empty_title), stringResource(R.string.music_apps_empty_message))
                    }
                    else -> {
                        item(key = "header") { SectionHeader(stringResource(R.string.music_apps_pick)) }
                        if (chosen.isEmpty()) {
                            item(key = "hint") {
                                Text(
                                    stringResource(R.string.music_apps_none_chosen),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                                )
                            }
                        }
                        itemsIndexed(list, key = { _, app -> app.first }) { index, (packageName, label) ->
                            val on = packageName in chosen
                            ConnectedItem(index = index, count = list.size) {
                                ConnectedRow(
                                    title = label,
                                    leading = { AppIcon(packageName) },
                                    trailing = { ExpressiveSwitch(checked = on, onCheckedChange = null) },
                                    role = Role.Switch,
                                    tapHaptic = false,
                                    onClick = {
                                        haptics.toggle(!on)
                                        chosen = if (on) chosen - packageName else chosen + packageName
                                        MusicApps.setChosen(context, packageName, !on)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

