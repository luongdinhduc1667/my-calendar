package vn.lichviet.app.ics

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventEntity
import vn.lichviet.app.data.EventMutationLock
import vn.lichviet.app.data.EventReminderEntity
import vn.lichviet.app.data.EventValidation
import vn.lichviet.app.reminder.ReminderScheduler
import vn.lichviet.app.widget.CalendarWidgets

enum class IcsImportChoice { SKIP, IMPORT, UPDATE }

data class IcsImportResult(val imported: Int, val updated: Int, val skipped: Int)

/** SAF transport and transactional persistence; iCalendar parsing lives in [IcsCodec]. */
@Singleton
class IcsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val database = AppDatabase.get(context)
    private val eventDao = database.eventDao()
    private val reminderDao = database.reminderDao()

    suspend fun preview(uri: Uri): IcsImportPreview = withContext(Dispatchers.IO) {
        val text = readUtf8(uri)
        IcsCodec.preview(text, eventDao.getAll().mapNotNull { event ->
            event.icsUid?.let { it to event.id }
        }.toMap(), vn.lichviet.app.AppLocaleManager.localizedContext(context).getString(vn.lichviet.app.R.string.ics_untitled))
    }

    suspend fun import(preview: IcsImportPreview, choices: Map<String, IcsImportChoice>): IcsImportResult =
        withContext(Dispatchers.IO) {
            EventMutationLock.mutex.withLock {
                withContext(NonCancellable) {
                    val cancelIds = mutableSetOf<Long>()
                    val scheduleIds = mutableSetOf<Long>()
                    var imported = 0
                    var updated = 0
                    var skipped = 0
                    database.withTransaction {
                        preview.items.forEach { item ->
                            val choice = choices[item.uid] ?: IcsImportChoice.SKIP
                            if (choice == IcsImportChoice.SKIP) {
                                skipped++
                                return@forEach
                            }
                            require(item.canImport) { "Sự kiện ICS không thể nhập an toàn: ${item.uid}" }
                            val existing = eventDao.getByUid(item.uid)
                            require(choice != IcsImportChoice.IMPORT || existing == null) { "UID đã tồn tại: ${item.uid}" }
                            require(choice != IcsImportChoice.UPDATE || existing != null) { "UID không còn tồn tại: ${item.uid}" }
                            val draft = EventValidation.validated(item.draft.copy(
                                id = existing?.id ?: 0,
                                icsUid = item.uid,
                                icsCalendar = item.draft.icsCalendar,
                                colorArgb = existing?.colorArgb ?: item.draft.colorArgb,
                            ))
                            val id = if (existing == null) eventDao.insert(draft) else {
                                check(eventDao.update(draft) == 1)
                                existing.id
                            }
                            cancelIds += reminderDao.forEvent(id).map { it.id }
                            reminderDao.deleteForEvent(id)
                            item.displayAlarmOffsetsMinutes.distinct().forEach { offset ->
                                reminderDao.insert(EventReminderEntity(eventId = id, offsetMinutes = offset))
                            }
                            scheduleIds += id
                            if (existing == null) imported++ else updated++
                        }
                    }
                    cancelIds.forEach { ReminderScheduler.cancelAlarm(context, it) }
                    scheduleIds.forEach { ReminderScheduler.scheduleLocked(context, it) }
                    if (scheduleIds.isNotEmpty()) CalendarWidgets.requestUpdate(context)
                    IcsImportResult(imported, updated, skipped)
                }
            }
        }

    suspend fun export(
        ids: Set<Long>, startInclusive: LocalDate? = null, endExclusive: LocalDate? = null,
    ): ByteArray = withContext(Dispatchers.Default) {
        require(ids.isNotEmpty() && ids.size <= 10_000) { "Hãy chọn sự kiện cần xuất" }
        val selected = eventDao.getAll().filter { it.id in ids }
        require(selected.size == ids.size) { "Một số sự kiện không còn tồn tại" }
        val reminders = reminderDao.getAll().groupBy({ it.eventId }, { it.offsetMinutes })
        IcsCodec.export(selected, startInclusive, endExclusive, reminders).toByteArray(StandardCharsets.UTF_8)
    }

    suspend fun exportAll(startInclusive: LocalDate? = null, endExclusive: LocalDate? = null): ByteArray =
        withContext(Dispatchers.Default) {
            val reminders = reminderDao.getAll().groupBy({ it.eventId }, { it.offsetMinutes })
            IcsCodec.export(eventDao.getAll(), startInclusive, endExclusive, reminders).toByteArray(StandardCharsets.UTF_8)
        }

    private fun readUtf8(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                require(result.size() + size <= MAX_ICS_BYTES) { "Tệp ICS quá lớn" }
                result.write(buffer, 0, size)
            }
            result.toByteArray()
        } ?: throw IllegalArgumentException("Không mở được tệp ICS")
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        } catch (error: CharacterCodingException) {
            throw IllegalArgumentException("Tệp ICS phải dùng UTF-8", error)
        }
    }

    companion object { const val MAX_ICS_BYTES = 4 * 1024 * 1024 }
}
