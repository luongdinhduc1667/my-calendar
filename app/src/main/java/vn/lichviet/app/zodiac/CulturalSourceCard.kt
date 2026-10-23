package vn.lichviet.app.zodiac

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.lichviet.app.R
import vn.lichviet.core.zodiac.CulturalRuleSet
import vn.lichviet.core.zodiac.CulturalSource

@Composable
internal fun CulturalSourceButton(vararg sources: CulturalSource, tag: String = "culture_sources") {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton({ open = true }, Modifier.testTag(tag)) { Text(stringResource(R.string.zd_sources)) }
    if (open) {
        val uri = LocalUriHandler.current
        var failed by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.zd_sources)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.zd_rule_set, CulturalRuleSet.ID))
                    Text(stringResource(R.string.zd_source_reviewed, CulturalRuleSet.SOURCES_REVIEWED_ON))
                    sources.distinct().forEach { source ->
                        Text(source.title, fontWeight = FontWeight.Bold)
                        Text(source.section)
                        Text(stringArrayResource(R.array.zd_source_scopes)[source.ordinal], style = MaterialTheme.typography.bodySmall)
                        SelectionContainer { Text(source.url, style = MaterialTheme.typography.bodySmall) }
                        TextButton({ failed = runCatching { uri.openUri(source.url) }.isFailure }) { Text(stringResource(R.string.zd_open_source)) }
                        HorizontalDivider()
                    }
                    if (failed) Text(stringResource(R.string.zd_source_error), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { TextButton({ open = false }) { Text(stringResource(R.string.zd_close)) } })
    }
}
