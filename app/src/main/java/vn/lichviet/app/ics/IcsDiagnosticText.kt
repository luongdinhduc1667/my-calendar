package vn.lichviet.app.ics

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import vn.lichviet.app.R

@Composable internal fun IcsDiagnosticMessage(issue: IcsDiagnostic, isError: Boolean = true) {
    val resource = when(issue.code) {
        IcsIssueCode.FILE_TOO_LARGE -> R.string.ics_issue_file_large
        IcsIssueCode.PARSE_ERROR -> R.string.ics_issue_parse
        IcsIssueCode.EVENT_LIMIT -> R.string.ics_issue_limit
        IcsIssueCode.MISSING_UID -> R.string.ics_issue_uid
        IcsIssueCode.NO_EVENTS -> R.string.ics_issue_empty
        IcsIssueCode.INVALID_MASTER -> R.string.ics_issue_master
        IcsIssueCode.OVERRIDE_LIMIT -> R.string.ics_issue_override_limit
        IcsIssueCode.INVALID_DATA -> R.string.ics_issue_invalid
        IcsIssueCode.UNSUPPORTED_FEATURE -> R.string.ics_issue_unsupported
        IcsIssueCode.ALARM_PARTIAL -> R.string.ics_issue_alarm
        IcsIssueCode.NATIVE_RESTORED -> R.string.ics_issue_native
        IcsIssueCode.NATIVE_IGNORED -> R.string.ics_issue_native_ignored
    }
    var details by remember(issue) { mutableStateOf(false) }
    Column {
        Text(stringResource(resource), style = MaterialTheme.typography.bodySmall,
            color = if(isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        issue.detail?.let {
            TextButton(onClick = { details = !details }) { Text(stringResource(R.string.ics_diagnostic_detail)) }
            if(details) Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}
