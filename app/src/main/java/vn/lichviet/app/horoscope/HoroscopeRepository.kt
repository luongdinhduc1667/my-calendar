package vn.lichviet.app.horoscope

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import vn.lichviet.app.data.AppDatabase
import vn.lichviet.app.data.EventMutationLock

class HoroscopeRepository(context: Context) {
    private val dao = AppDatabase.get(context.applicationContext).horoscopeProfileDao()
    val profiles = dao.observeAll()

    suspend fun save(profile: HoroscopeProfileEntity): Long = withContext(Dispatchers.IO) {
        val checked = HoroscopeProfileValidation.validated(profile)
        EventMutationLock.mutex.withLock { withContext(NonCancellable) { dao.save(checked) } }
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        EventMutationLock.mutex.withLock { withContext(NonCancellable) { dao.delete(id) } }
    }
}
