package vn.lichviet.app.data

import kotlinx.coroutines.sync.Mutex

/** Serialize a notification with edits/deletion so stale event content cannot survive CRUD. */
internal object EventMutationLock {
    val mutex = Mutex()
}
