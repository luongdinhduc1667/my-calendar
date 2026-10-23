package vn.lichviet.app.horoscope

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.lichviet.app.R
import vn.lichviet.core.horoscope.HoroscopeChart
import vn.lichviet.core.horoscope.HoroscopeChartEngine

data class HoroscopeSelection(val profile: HoroscopeProfileEntity, val chart: HoroscopeChart)
data class HoroscopeOperation(val running: Boolean = false, val completed: Boolean = false, val error: Int? = null)
data class HoroscopeExport(val file: File, val format: ChartExportFormat, val share: Boolean)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HoroscopeViewModel(private val repository: HoroscopeRepository, private val state: SavedStateHandle) : ViewModel() {
    val profiles = repository.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val selectedId = state.getStateFlow("horoscope_selected_id", 0L)
    val selection = combine(profiles, selectedId) { list, id -> list.firstOrNull { it.id == id } ?: list.firstOrNull() }
        .mapLatest { profile -> profile?.let { withContext(Dispatchers.Default) {
            HoroscopeSelection(it, HoroscopeChartEngine.calculate(HoroscopeProfileValidation.birthProfile(it)))
        } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _operation = MutableStateFlow(HoroscopeOperation())
    val operation = _operation.asStateFlow()
    private val _exportReady = MutableStateFlow<HoroscopeExport?>(null)
    val exportReady = _exportReady.asStateFlow()
    private val _notice = MutableStateFlow<Int?>(null)
    val notice = _notice.asStateFlow()
    private var pendingExport: HoroscopeExport? = null

    fun select(id: Long) { state["horoscope_selected_id"] = id }
    fun resetOperation() { if (!_operation.value.running) _operation.value = HoroscopeOperation() }
    fun clearNotice() { _notice.value = null }
    fun save(entity: HoroscopeProfileEntity) {
        if (_operation.value.running) return
        _operation.value = HoroscopeOperation(running = true)
        viewModelScope.launch {
            try {
                select(repository.save(entity))
                _operation.value = HoroscopeOperation(completed = true)
            } catch (error: CancellationException) { throw error }
            catch (error: IllegalArgumentException) { _operation.value = HoroscopeOperation(error = R.string.hs_invalid) }
            catch (error: Exception) { _operation.value = HoroscopeOperation(error = R.string.hs_storage_error) }
        }
    }
    fun delete(id: Long) {
        if (_operation.value.running) return
        _operation.value = HoroscopeOperation(running = true)
        viewModelScope.launch {
            try {
                repository.delete(id)
                _operation.value = HoroscopeOperation(completed = true)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _operation.value = HoroscopeOperation(error = R.string.hs_storage_error) }
        }
    }

    /** Called only after the screen's explicit personal-data confirmation. */
    fun prepareExport(context: Context, selection: HoroscopeSelection, format: ChartExportFormat, share: Boolean) {
        if (_operation.value.running) return
        _operation.value = HoroscopeOperation(running = true)
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { ChartExporter.create(context, selection.profile, selection.chart, format) }
                val export = HoroscopeExport(file, format, share)
                pendingExport = export
                _exportReady.value = export
                _operation.value = HoroscopeOperation()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _operation.value = HoroscopeOperation(error = R.string.hs_storage_error) }
        }
    }
    fun exportLaunched() { _exportReady.value = null }
    fun exportFailed() { _notice.value = R.string.hs_storage_error }
    fun exportTo(context: Context, uri: Uri?) {
        if (uri == null) return
        val export = pendingExport ?: run { _notice.value = R.string.hs_storage_error; return }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt").use { output ->
                        checkNotNull(output)
                        export.file.inputStream().use { input -> input.copyTo(output) }
                    }
                }
                _notice.value = R.string.hs_export_ready
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _notice.value = R.string.hs_storage_error }
        }
    }
    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { HoroscopeViewModel(HoroscopeRepository(context), createSavedStateHandle()) }
        }
    }
}
