package vn.lichviet.app.ics

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.lichviet.app.R
import vn.lichviet.app.AppLocaleManager

/** Retains previews and in-flight work when configuration changes recreate the Activity. */
class IcsViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = IcsRepository(application)
    private val mutablePreview = MutableStateFlow<IcsImportPreview?>(null)
    val preview = mutablePreview.asStateFlow()
    private val mutableChoices = MutableStateFlow<Map<String, IcsImportChoice>>(emptyMap())
    val choices = mutableChoices.asStateFlow()
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()
    private val mutableShare = MutableStateFlow<Uri?>(null)
    val share = mutableShare.asStateFlow()
    init { saved.get<String>("source")?.let { load(Uri.parse(it), restoring = true) } }

    fun load(uri: Uri, restoring: Boolean = false) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        saved["source"] = uri.toString()
        viewModelScope.launch {
            try {
                val parsed = repository.preview(uri)
                mutablePreview.value = parsed
                val priorUids = saved.get<ArrayList<String>>("choice_uids").orEmpty()
                val priorValues = saved.get<ArrayList<String>>("choice_values").orEmpty()
                val previous = if (restoring) priorUids.zip(priorValues).toMap() else emptyMap()
                mutableChoices.value = parsed.items.associate { item -> item.uid to
                    (previous[item.uid]?.let { runCatching { IcsImportChoice.valueOf(it) }.getOrNull() }
                        ?: if (item.canImport && item.duplicateEventId == null) IcsImportChoice.IMPORT else IcsImportChoice.SKIP) }
                persistChoices()
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableMessage.value = AppLocaleManager.localizedContext(getApplication<Application>()).getString(R.string.ics_read_failed); saved["source"] = null }
            finally { mutableBusy.value = false }
        }
    }
    fun choose(uid: String, choice: IcsImportChoice) { mutableChoices.value += uid to choice; persistChoices() }
    private fun persistChoices() {
        saved["choice_uids"] = ArrayList(mutableChoices.value.keys)
        saved["choice_values"] = ArrayList(mutableChoices.value.values.map { it.name })
    }
    fun dismissPreview() { if (!mutableBusy.value) { mutablePreview.value = null; saved["source"] = null } }
    fun dismissMessage() { mutableMessage.value = null }
    fun showMessage(text: String) { mutableMessage.value = text }
    fun consumeShare() { mutableShare.value = null }
    fun apply() {
        val parsed = mutablePreview.value ?: return
        if (mutableBusy.value) return
        val selected = mutableChoices.value
        mutableBusy.value = true
        viewModelScope.launch {
            try {
                val result = repository.import(parsed, selected)
                mutablePreview.value = null; saved["source"] = null
                mutableMessage.value = AppLocaleManager.localizedContext(getApplication<Application>()).getString(R.string.ics_imported, result.imported, result.updated, result.skipped)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableMessage.value = AppLocaleManager.localizedContext(getApplication<Application>()).getString(R.string.ics_import_failed) }
            finally { mutableBusy.value = false }
        }
    }
    fun export(ids: Set<Long>, from: LocalDate?, until: LocalDate?, target: Uri?) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        viewModelScope.launch {
            try {
                val bytes = repository.export(ids, from, until)
                val application = getApplication<Application>()
                if (target == null) {
                    val uri = withContext(Dispatchers.IO) {
                        val directory = File(application.cacheDir, "ics").apply { mkdirs() }
                        // Keep grants to older shares valid for one day; bound cache growth on later shares.
                        directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000L }?.forEach { it.delete() }
                        val file = File(directory, "lich-viet-${System.nanoTime()}.ics").apply { writeBytes(bytes) }
                        FileProvider.getUriForFile(application, "${application.packageName}.fileprovider", file)
                    }
                    mutableShare.value = uri
                } else {
                    withContext(Dispatchers.IO) {
                        application.contentResolver.openOutputStream(target, "wt")?.use { it.write(bytes) } ?: error("No output stream")
                    }
                    mutableMessage.value = AppLocaleManager.localizedContext(application).getString(R.string.ics_exported)
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableMessage.value = AppLocaleManager.localizedContext(getApplication<Application>()).getString(R.string.ics_export_failed) }
            finally { mutableBusy.value = false }
        }
    }
}
