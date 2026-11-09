package dev.qtremors.osyster.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.device.domain.HomeLayoutRepository
import dev.qtremors.osyster.core.device.domain.PinKey
import dev.qtremors.osyster.core.navigation.Tool
import dev.qtremors.osyster.core.privilege.domain.PrivilegeStatus
import dev.qtremors.osyster.core.privilege.domain.PrivilegedShell
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Tools tab needs which tools are pinned to Home, and what the power tools can use right now. */
@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val layoutRepository: HomeLayoutRepository,
    private val shell: PrivilegedShell
) : ViewModel() {
    val access: StateFlow<PrivilegeStatus> = shell.status

    /** Access can change outside Osyster (Shizuku stopped, permission revoked), so re-check on return. */
    fun refreshAccess() = shell.refresh()

    val pinnedTools: StateFlow<Set<String>> = layoutRepository.layout
        .map { layout -> layout.pinned.filter { it.kind == PinKey.Kind.TOOL }.map(PinKey::id).toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun togglePin(tool: Tool) {
        viewModelScope.launch { layoutRepository.update { it.togglePin(PinKey.tool(tool.id)) } }
    }
}
