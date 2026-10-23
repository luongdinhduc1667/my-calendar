package vn.lichviet.app.ics

import vn.lichviet.app.data.EventEntity
import java.time.Instant
import java.time.LocalDate

/** A source event expanded to an actual calendar instance. End values are exclusive. */
data class EventOccurrence(
    val event: EventEntity,
    val occurrenceKey: String,
    val start: Instant,
    val endExclusive: Instant,
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
    val title: String,
    val note: String,
    val location: String,
    val allDay: Boolean,
    val colorArgb: Int?,
) {
    val eventId: Long get() = event.id
    val date: LocalDate get() = startDate
}

data class IcsImportPreview(
    val items: List<IcsPreviewItem>,
    val fileErrors: List<String> = emptyList(),
    val fileDiagnostics: List<IcsDiagnostic> = emptyList(),
)

data class IcsPreviewItem(
    val uid: String,
    val title: String,
    val canonicalCalendar: String,
    val draft: EventEntity,
    val duplicateEventId: Long?,
    val displayAlarmOffsetsMinutes: List<Int>,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val diagnostics: List<IcsDiagnostic> = emptyList(),
) {
    val canImport: Boolean get() = errors.isEmpty()
}

enum class IcsIssueCode {
    FILE_TOO_LARGE, PARSE_ERROR, EVENT_LIMIT, MISSING_UID, NO_EVENTS,
    INVALID_MASTER, OVERRIDE_LIMIT, INVALID_DATA, UNSUPPORTED_FEATURE,
    ALARM_PARTIAL, NATIVE_RESTORED, NATIVE_IGNORED,
}
data class IcsDiagnostic(val code: IcsIssueCode, val detail: String? = null)

/** Raised when a series cannot be expanded without changing its RFC 5545 meaning. */
class UnsupportedIcsException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
