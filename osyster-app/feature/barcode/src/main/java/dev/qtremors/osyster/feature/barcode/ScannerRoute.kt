package dev.qtremors.osyster.feature.barcode

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.barcode.domain.ScanHistory
import dev.qtremors.osyster.core.barcode.domain.ScanRecord
import dev.qtremors.osyster.core.barcode.domain.ScannerSettings
import dev.qtremors.osyster.core.barcode.domain.ScannerSettingsStore
import dev.qtremors.osyster.core.barcode.domain.SearchSite
import dev.qtremors.osyster.core.barcode.domain.SearchSiteStore
import dev.qtremors.osyster.core.navigation.BarcodeHistoryRoute
import dev.qtremors.osyster.core.navigation.BarcodeRoute
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val history: ScanHistory,
    private val siteStore: SearchSiteStore,
    private val settingsStore: ScannerSettingsStore
) : ViewModel() {
    val settings: StateFlow<ScannerSettings> = settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, ScannerSettings())
    val sites: StateFlow<List<SearchSite>> = siteStore.sites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val records: StateFlow<List<ScanRecord>?> = history.records.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The code on show in the result sheet, from a scan or from the history. */
    private val _shown = MutableStateFlow<ReadCode?>(null)
    val shown: StateFlow<ReadCode?> = _shown.asStateFlow()

    /** A new scan: shown and saved to the history. */
    fun onRead(code: ReadCode) {
        _shown.value = code
        viewModelScope.launch { history.add(code.text, code.format, System.currentTimeMillis()) }
    }

    /** A past scan, shown again without saving it twice. */
    fun show(record: ScanRecord) {
        _shown.value = ReadCode(record.text, record.format)
    }

    fun dismiss() {
        _shown.value = null
    }

    fun rename(id: Long, name: String?) {
        viewModelScope.launch { history.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { history.delete(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { history.clear() }
    }

    fun addSite(name: String, template: String) {
        viewModelScope.launch { siteStore.add(name, template) }
    }

    fun deleteSite(id: Long) {
        viewModelScope.launch { siteStore.delete(id) }
    }

    fun update(transform: (ScannerSettings) -> ScannerSettings) {
        viewModelScope.launch { settingsStore.update(transform) }
    }
}

sealed interface BarcodeDestination {
    data object History : BarcodeDestination

    /** Osyster's App info page, where a camera permission refused twice can still be turned on. */
    data class AppSettings(val packageName: String) : BarcodeDestination
}

fun NavGraphBuilder.registerBarcodeRoutes(onNavigate: (BarcodeDestination) -> Unit) {
    composable<BarcodeRoute> {
        val context = LocalContext.current
        ScannerScreen(
            viewModel = hiltViewModel(),
            onHistory = { onNavigate(BarcodeDestination.History) },
            onAppSettings = { onNavigate(BarcodeDestination.AppSettings(context.packageName)) }
        )
    }
    composable<BarcodeHistoryRoute> {
        HistoryScreen(viewModel = hiltViewModel())
    }
}
