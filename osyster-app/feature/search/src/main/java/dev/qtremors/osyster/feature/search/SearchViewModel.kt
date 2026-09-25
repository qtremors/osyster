package dev.qtremors.osyster.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.DevicePage
import dev.qtremors.osyster.core.device.domain.DevicePageSource
import dev.qtremors.osyster.core.runtime.runCatchingCancellable
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** A device fact as search sees it: which page it lives on, and its label and value. */
data class FactHit(val pageId: String, val key: String, val label: String, val value: String)

/**
 * Collects the facts of every built device page once, so search can match values ("Snapdragon",
 * "Li-ion") as well as page names. Live pages contribute their first reading; a page that does
 * not answer quickly is skipped rather than holding up the results.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(private val pages: DevicePageSource) : ViewModel() {
    private val _facts = MutableStateFlow<List<FactHit>>(emptyList())
    val facts: StateFlow<List<FactHit>> = _facts.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        viewModelScope.launch {
            _facts.value = DevicePage.entries
                .filter(pages::isBuilt)
                .map { page -> async { factsOf(page) } }
                .awaitAll()
                .flatten()
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    private suspend fun factsOf(page: DevicePage): List<FactHit> {
        val sections = withTimeoutOrNull(PAGE_TIMEOUT_MS) {
            runCatchingCancellable { pages.sections(page).first() }.getOrNull()
        } ?: return emptyList()
        return sections.flatMap { section ->
            section.rows.mapNotNull { row ->
                val value = (row.value as? Availability.Available)?.value ?: return@mapNotNull null
                FactHit(page.id, "${page.id}:${section.key}:${row.key}", row.label, value)
            }
        }
    }

    private companion object {
        const val PAGE_TIMEOUT_MS = 2_500L
    }
}
