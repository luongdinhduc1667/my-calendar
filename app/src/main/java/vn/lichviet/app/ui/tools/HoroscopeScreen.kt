package vn.lichviet.app.ui.tools

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import vn.lichviet.app.R
import vn.lichviet.app.horoscope.HoroscopeWorkspace
import vn.lichviet.app.zodiac.ZodiacWorkspace
import vn.lichviet.app.zodiac.FengShuiWorkspace

@Composable
fun HoroscopeScreen(date: LocalDate, onOpenDate: (LocalDate) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val holder = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.zd_tab_zodiac, R.string.zd_tab_chart, R.string.zd_tab_feng_shui).forEachIndexed { index, title ->
                FilterChip(tab == index, { tab = index }, { Text(stringResource(title)) }, Modifier.testTag("hs_section_$index"))
            }
        }
        Box(Modifier.weight(1f)) {
            holder.SaveableStateProvider(tab) {
                when (tab) {
                    0 -> ZodiacWorkspace(date, onOpenDate)
                    1 -> HoroscopeWorkspace()
                    else -> FengShuiWorkspace(date)
                }
            }
        }
    }
}
