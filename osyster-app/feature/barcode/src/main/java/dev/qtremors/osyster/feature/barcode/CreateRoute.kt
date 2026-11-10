package dev.qtremors.osyster.feature.barcode

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.osyster.core.barcode.domain.CodeField
import dev.qtremors.osyster.core.barcode.domain.CodeMatrix
import dev.qtremors.osyster.core.barcode.domain.CodeStyle
import dev.qtremors.osyster.core.barcode.domain.CreateKind
import dev.qtremors.osyster.core.barcode.domain.CreateProblem
import dev.qtremors.osyster.core.barcode.domain.Encoded
import dev.qtremors.osyster.core.barcode.domain.Recovery
import dev.qtremors.osyster.core.barcode.domain.WifiSecurity
import dev.qtremors.osyster.core.barcode.domain.encode
import dev.qtremors.osyster.core.barcode.domain.encodeMatrix
import dev.qtremors.osyster.core.navigation.BarcodeCreateFormRoute
import dev.qtremors.osyster.core.runtime.di.AppDispatchers
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/** A created code as drawn on screen: the text it holds, or why there is none yet. */
internal data class CodePreview(
    val text: String? = null,
    val matrix: CodeMatrix? = null,
    val image: Bitmap? = null,
    val problem: CreateProblem? = null
)

/** Export sizes, the width of the saved or shared picture in pixels. */
internal enum class ExportSize(val px: Int) { SMALL(512), MEDIUM(1024), LARGE(2048) }

/**
 * One creator form. Field values and the style are kept in the saved state, so a half-filled form
 * survives rotation and the app being closed in the background. The code is redrawn off the main
 * thread a moment after typing stops.
 */
@HiltViewModel
class CreateViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val dispatchers: AppDispatchers
) : ViewModel() {
    internal val kind: CreateKind = CreateKind.named(savedState.toRoute<BarcodeCreateFormRoute>().kind) ?: CreateKind.TEXT

    private val _values = MutableStateFlow(restoreValues())
    internal val values: StateFlow<Map<CodeField, String>> = _values.asStateFlow()

    private val _style = MutableStateFlow(restoreStyle())
    internal val style: StateFlow<CodeStyle> = _style.asStateFlow()

    private val _size = MutableStateFlow(savedState.get<String>(KEY_SIZE)?.let { name -> ExportSize.entries.firstOrNull { it.name == name } } ?: ExportSize.MEDIUM)
    internal val size: StateFlow<ExportSize> = _size.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    internal val preview: StateFlow<CodePreview> = combine(_values, _style, ::Pair)
        .debounce(PREVIEW_DELAY_MS)
        .mapLatest { (values, style) -> withContext(dispatchers.default) { draw(values, style) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CodePreview())

    internal fun set(field: CodeField, value: String) {
        _values.value = _values.value + (field to value)
        savedState[KEY_VALUES] = HashMap(_values.value.mapKeys { it.key.name })
    }

    internal fun style(transform: (CodeStyle) -> CodeStyle) {
        val next = transform(_style.value)
        _style.value = next
        savedState[KEY_STYLE] = intArrayOf(next.foreground, next.background, next.margin, if (next.rounded) 1 else 0, if (next.showText) 1 else 0, next.recovery.ordinal)
    }

    internal fun size(size: ExportSize) {
        _size.value = size
        savedState[KEY_SIZE] = size.name
    }

    /** The picture to save or share, at the chosen export size. */
    internal suspend fun export(): Bitmap? {
        val shown = preview.value
        val matrix = shown.matrix ?: return null
        return withContext(dispatchers.default) { CodeImage.draw(matrix, _style.value, shown.text.orEmpty(), _size.value.px) }
    }

    private fun draw(values: Map<CodeField, String>, style: CodeStyle): CodePreview =
        when (val encoded = encode(kind, values)) {
            is Encoded.Invalid -> CodePreview(problem = encoded.problem)
            is Encoded.Ready -> {
                val matrix = encodeMatrix(encoded.text, kind.format, style.recovery)
                if (matrix == null) {
                    CodePreview(problem = CreateProblem.TooLong)
                } else {
                    CodePreview(encoded.text, matrix, CodeImage.draw(matrix, style, encoded.text, PREVIEW_PX))
                }
            }
        }

    private fun restoreValues(): Map<CodeField, String> {
        savedState.get<HashMap<String, String>>(KEY_VALUES)?.let { saved ->
            return saved.mapNotNull { (key, value) -> CodeField.entries.firstOrNull { it.name == key }?.let { it to value } }.toMap()
        }
        return when (kind) {
            CreateKind.WIFI -> mapOf(CodeField.SECURITY to WifiSecurity.WPA.name)
            CreateKind.EVENT -> {
                // Starts at the next full hour where the person is (not in UTC, which is half an
                // hour off in some places) and lasts an hour.
                val next = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1)
                mapOf(CodeField.START to "${next.toInstant().toEpochMilli()}", CodeField.END to "${next.plusHours(1).toInstant().toEpochMilli()}")
            }
            else -> emptyMap()
        }
    }

    private fun restoreStyle(): CodeStyle {
        val saved = savedState.get<IntArray>(KEY_STYLE) ?: return CodeStyle.defaultsFor(kind.format)
        return CodeStyle(
            foreground = saved[0],
            background = saved[1],
            margin = saved[2],
            rounded = saved[3] == 1,
            showText = saved[4] == 1,
            recovery = Recovery.entries.getOrElse(saved[5]) { Recovery.MEDIUM }
        )
    }

    private companion object {
        const val KEY_VALUES = "values"
        const val KEY_STYLE = "style"
        const val KEY_SIZE = "size"
        const val PREVIEW_DELAY_MS = 150L
        const val PREVIEW_PX = 720
    }
}
