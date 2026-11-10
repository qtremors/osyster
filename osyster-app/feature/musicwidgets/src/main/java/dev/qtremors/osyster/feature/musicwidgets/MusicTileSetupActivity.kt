package dev.qtremors.osyster.feature.musicwidgets

import dev.qtremors.osyster.core.ui.components.NavigationRow
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.Icons
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.osyster.core.ui.components.ChoiceRow
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.ToggleRow
import dev.qtremors.osyster.core.ui.theme.OsysterTheme
import dev.qtremors.osyster.core.ui.theme.ThemeSettings
import dev.qtremors.osyster.core.ui.theme.spacing
import javax.inject.Inject

/**
 * Opens when the music tiles widget is placed (and from its widget settings on Android 12+): its
 * size, where the play controls and side buttons go, and which apps it follows, with the real
 * widget as a live preview. Backing out cancels placing the widget.
 */
@AndroidEntryPoint
class MusicTileSetupActivity : ComponentActivity() {
    @Inject lateinit var themeSettings: ThemeSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        val initial = MusicTileSettings.read(this, id)
        // The arrangement this widget shows at its current size, which Auto previews first.
        val current = initial.copy(size = TileSize.AUTO).kind(MusicTileRenderer.heightDp(AppWidgetManager.getInstance(this), id))
        val done = if (MusicTileSettings.has(this, id)) R.string.music_setup_save else R.string.music_setup_add
        setContent {
            val options by themeSettings.options.collectAsStateWithLifecycle(initialValue = null)
            val loaded = options ?: return@setContent
            OsysterTheme(options = loaded) {
                MusicTileSetupScreen(current, id, initial, stringResource(done), onApps = { startActivity(Intent(this, MusicAppsActivity::class.java)) }) { config -> save(id, config) }
            }
        }
    }

    private fun save(id: Int, config: MusicTileConfig) {
        MusicTileSettings.write(this, id, config)
        MusicTileRenderer.update(this, AppWidgetManager.getInstance(this), id)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        finish()
    }
}

@Composable
private fun MusicTileSetupScreen(
    current: MusicTileKind,
    id: Int,
    initial: MusicTileConfig,
    doneLabel: String,
    onApps: () -> Unit,
    onDone: (MusicTileConfig) -> Unit
) {
    var controlsTop by rememberSaveable { mutableStateOf(initial.controlsTop) }
    var sideLeft by rememberSaveable { mutableStateOf(initial.sideLeft) }
    var buttons by rememberSaveable { mutableStateOf(initial.buttons) }
    var playBySide by rememberSaveable { mutableStateOf(initial.playBySide) }
    var size by rememberSaveable { mutableStateOf(initial.size) }
    // With Auto, both arrangements can show, so either can be previewed and set up here.
    var previewed by rememberSaveable { mutableStateOf(current) }
    val config = MusicTileConfig(controlsTop, sideLeft, buttons, playBySide, size)
    val kind = when (size) {
        TileSize.AUTO -> previewed
        else -> config.kind(null)
    }
    val direction = LocalLayoutDirection.current
    val spacing = MaterialTheme.spacing
    val title = stringResource(R.string.widget_music_grid_name)

    OsysterScaffold(title = title) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = padding.calculateStartPadding(direction), end = padding.calculateEndPadding(direction), top = padding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                TilePreview(kind, config, id)
                ConnectedGroup {
                    ChoiceRow(
                        title = stringResource(R.string.music_setup_size),
                        supporting = stringResource(if (size == TileSize.AUTO) R.string.music_setup_size_auto_summary else R.string.music_setup_size_fixed_summary),
                        options = TileSize.entries,
                        selected = size,
                        label = { stringResource(it.label) },
                        onSelect = { size = it }
                    )
                    if (size == TileSize.AUTO) {
                        ChoiceRow(
                            title = stringResource(R.string.music_setup_previewing),
                            options = MusicTileKind.entries,
                            selected = previewed,
                            label = { stringResource(if (it == MusicTileKind.GRID) R.string.music_setup_size_large else R.string.music_setup_size_small) },
                            onSelect = { previewed = it }
                        )
                    }
                }
                ConnectedGroup {
                    if (kind == MusicTileKind.GRID) {
                        ChoiceRow(
                            title = stringResource(R.string.music_setup_controls),
                            supporting = stringResource(R.string.music_setup_controls_summary),
                            options = listOf(true, false),
                            selected = controlsTop,
                            label = { stringResource(if (it) R.string.music_setup_top else R.string.music_setup_bottom) },
                            onSelect = { controlsTop = it }
                        )
                    }
                    ChoiceRow(
                        title = stringResource(R.string.music_setup_side),
                        supporting = stringResource(R.string.music_setup_side_summary),
                        options = listOf(true, false),
                        selected = sideLeft,
                        label = { stringResource(if (it) R.string.music_setup_left else R.string.music_setup_right) },
                        onSelect = { sideLeft = it }
                    )
                    if (kind == MusicTileKind.GRID) {
                        ToggleRow(
                            title = stringResource(R.string.music_setup_play_side),
                            supporting = stringResource(R.string.music_setup_play_side_summary),
                            checked = playBySide,
                            onCheckedChange = { playBySide = it }
                        )
                    }
                    if (kind == MusicTileKind.TILES) {
                        ChoiceRow(
                            title = stringResource(R.string.music_setup_buttons),
                            options = SideButtons.entries,
                            selected = buttons,
                            label = { stringResource(if (it == SideButtons.SHUFFLE_REPEAT) R.string.music_setup_shuffle_repeat else R.string.music_setup_play_repeat) },
                            onSelect = { buttons = it }
                        )
                    }
                }
                ConnectedGroup {
                    NavigationRow(
                        stringResource(R.string.music_setup_apps),
                        Icons.Rounded.LibraryMusic,
                        "music-apps",
                        onApps,
                        supporting = stringResource(R.string.music_setup_apps_summary)
                    )
                }
            }
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = { onDone(config) },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = spacing.medium).height(56.dp)
            ) { Text(doneLabel, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

/** The real widget layout, filled the same way the widget is, sized like 3x3 or 3x2 cells. */
@Composable
private fun TilePreview(kind: MusicTileKind, config: MusicTileConfig, id: Int) {
    val context = LocalContext.current
    val description = stringResource(R.string.music_setup_preview)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 24.dp).semantics { contentDescription = description }) {
            AndroidView(
                factory = { FrameLayout(it) },
                modifier = Modifier.size(width = 264.dp, height = if (kind == MusicTileKind.GRID) 264.dp else 176.dp),
                update = { frame ->
                    val views = RemoteViews(context.packageName, config.layout(kind))
                    MusicTileRenderer.fill(context, views, kind, config, id)
                    frame.removeAllViews()
                    frame.addView(views.apply(context, frame))
                }
            )
        }
    }
}

private val TileSize.label: Int
    get() = when (this) {
        TileSize.AUTO -> R.string.music_setup_size_auto
        TileSize.LARGE -> R.string.music_setup_size_large
        TileSize.SMALL -> R.string.music_setup_size_small
    }
