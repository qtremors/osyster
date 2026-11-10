package dev.qtremors.osyster.feature.linkactions

import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.navigation.LinkActionsRoute
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.system.domain.LinkHandlers
import dev.qtremors.osyster.core.ui.catalog.label
import dev.qtremors.osyster.core.ui.components.ConnectedGroup
import dev.qtremors.osyster.core.ui.components.ConnectedRow
import dev.qtremors.osyster.core.ui.components.IconBadge
import dev.qtremors.osyster.core.ui.components.InlineSearchField
import dev.qtremors.osyster.core.ui.components.OsysterScaffold
import dev.qtremors.osyster.core.ui.components.SectionHeader
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class LinkActionsViewModel @Inject constructor(val links: LinkHandlers) : ViewModel() {
    private val _isDefault = MutableStateFlow(links.isDefaultForLinks())
    val isDefault: StateFlow<Boolean> = _isDefault.asStateFlow()
    fun refresh() {
        _isDefault.value = links.isDefaultForLinks()
    }
}

sealed interface LinkActionsDestination {
    data object DefaultApps : LinkActionsDestination
    data class Share(val text: String) : LinkActionsDestination
}

fun NavGraphBuilder.registerLinkActionsRoute(onNavigate: (LinkActionsDestination) -> Unit) {
    composable<LinkActionsRoute> {
        val viewModel: LinkActionsViewModel = hiltViewModel()
        val isDefault by viewModel.isDefault.collectAsStateWithLifecycle()
        LifecycleResumeEffect(Unit) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        LinkActionsScreen(isDefault = isDefault, links = viewModel.links, onNavigate = onNavigate)
    }
}

@Composable
private fun LinkActionsScreen(isDefault: Boolean, links: LinkHandlers, onNavigate: (LinkActionsDestination) -> Unit) {
    var testUrl by rememberSaveable { mutableStateOf("https://example.com") }
    var chooserFor by rememberSaveable { mutableStateOf<String?>(null) }
    OsysterScaffold(title = stringResource(Tool.LINK_ACTIONS.label.title)) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "status") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    ConnectedGroup {
                        ConnectedRow(
                            title = stringResource(if (isDefault) R.string.link_status_on else R.string.link_status_off),
                            supporting = stringResource(if (isDefault) R.string.link_status_on_detail else R.string.link_status_off_detail),
                            leading = { IconBadge(if (isDefault) Icons.Rounded.CheckCircle else Icons.Rounded.Link, key = "link_status") }
                        )
                        ConnectedRow(
                            title = stringResource(R.string.link_default_apps),
                            supporting = stringResource(R.string.link_default_apps_detail),
                            leading = { IconBadge(Icons.Rounded.SettingsApplications, key = "default_apps") },
                            onClick = { onNavigate(LinkActionsDestination.DefaultApps) }
                        )
                    }
                }
            }
            item(key = "try") {
                Column {
                    SectionHeader(stringResource(R.string.link_try))
                    InlineSearchField(query = testUrl, onQueryChange = { testUrl = it }, placeholder = stringResource(R.string.link_try_placeholder))
                    Button(shapes = ButtonDefaults.shapes(), 
                        onClick = { chooserFor = testUrl.trim() },
                        enabled = testUrl.trim().startsWith("http"),
                        modifier = Modifier.padding(top = 12.dp)
                    ) { androidx.compose.material3.Text(stringResource(R.string.link_try_button)) }
                }
            }
        }
    }
    chooserFor?.let { url ->
        LinkChooserSheet(
            url = url,
            links = links,
            onShare = { onNavigate(LinkActionsDestination.Share(it)) },
            onDone = { chooserFor = null }
        )
    }
}
