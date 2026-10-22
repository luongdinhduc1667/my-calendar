package vn.lichviet.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import vn.lichviet.app.data.*
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import java.time.YearMonth

data class EditorOperation(val running:Boolean=false,val completed:Boolean=false,val requestNotifications:Boolean=false,val error:String?=null)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: EventRepository,
    private val preferencesRepository: PreferencesRepository,
    private val savedState: SavedStateHandle,
    private val backupRepository: BackupRepository
) : ViewModel() {
    private fun today() = LocalDate.now(VietnameseCalendar.zoneId).coerceIn(VietnameseCalendar.minDate, VietnameseCalendar.maxDate)
    private val initialDate = runCatching { LocalDate.parse(savedState.get<String>("selected")) }.getOrNull()
        ?.takeIf { it in VietnameseCalendar.minDate..VietnameseCalendar.maxDate } ?: today()
    private val initialMonth = runCatching { YearMonth.parse(savedState.get<String>("month")) }.getOrNull()
        ?.takeIf { it.year in 1900..2100 } ?: YearMonth.from(initialDate)
    init {
        savedState["selected"] = initialDate.toString()
        savedState["month"] = initialMonth.toString()
    }
    val selected = savedState.getStateFlow("selected", initialDate.toString()).map(LocalDate::parse)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialDate)
    val month = savedState.getStateFlow("month", initialMonth.toString()).map(YearMonth::parse)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialMonth)
    val weekMode = savedState.getStateFlow("week", false)
    val events = repository.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val preferences = preferencesRepository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())
    private val _editorOperation=MutableStateFlow(EditorOperation())
    val editorOperation:StateFlow<EditorOperation> = _editorOperation.asStateFlow()
    fun select(date: LocalDate) {
        if(date !in VietnameseCalendar.minDate..VietnameseCalendar.maxDate) return
        savedState["selected"] = date.toString()
        savedState["month"] = YearMonth.from(date).toString()
    }
    fun changeMonth(value: YearMonth) {
        if(value.year !in 1900..2100) return
        savedState["month"] = value.toString()
    }
    fun changeWeekMode(value:Boolean) {
        if(value)savedState["month"] = YearMonth.from(LocalDate.parse(savedState.get<String>("selected")!!)).toString()
        savedState["week"] = value
    }
    fun goToday() = select(today())
    suspend fun save(event:EventEntity) = repository.save(event)
    suspend fun delete(id:Long) = repository.delete(id)
    fun saveFromEditor(event:EventEntity) {
        if(_editorOperation.value.running)return
        _editorOperation.value=EditorOperation(running=true)
        viewModelScope.launch {
            try {
                repository.save(event)
                _editorOperation.value=EditorOperation(completed=true,requestNotifications=event.reminderEnabled)
            }catch(e:CancellationException){throw e}
            catch(e:Exception){_editorOperation.value=EditorOperation(error="Không thể lưu sự kiện. Vui lòng thử lại.")}
        }
    }
    fun deleteFromEditor(id:Long) {
        if(_editorOperation.value.running)return
        _editorOperation.value=EditorOperation(running=true)
        viewModelScope.launch {
            try{repository.delete(id);_editorOperation.value=EditorOperation(completed=true)}
            catch(e:CancellationException){throw e}
            catch(e:Exception){_editorOperation.value=EditorOperation(error="Không thể xóa sự kiện. Vui lòng thử lại.")}
        }
    }
    fun resetEditorOperation(){if(!_editorOperation.value.running)_editorOperation.value=EditorOperation()}
    suspend fun setTheme(value:AppTheme) = preferencesRepository.setTheme(value)
    suspend fun setLunar(value:Boolean) = preferencesRepository.setShowLunar(value)
    suspend fun setHolidays(value:Boolean) = preferencesRepository.setShowHolidays(value)
    suspend fun setMonday(value:Boolean) = preferencesRepository.setMondayFirst(value)
    suspend fun exportBackup() = backupRepository.exportJson()
    suspend fun importBackup(json:String) = backupRepository.importJson(json)
    suspend fun deleteAllData() = backupRepository.deleteAllData()
}
