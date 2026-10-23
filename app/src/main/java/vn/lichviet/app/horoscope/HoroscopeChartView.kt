package vn.lichviet.app.horoscope

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.lichviet.app.R
import vn.lichviet.core.horoscope.*

@Composable
internal fun HoroscopeChartView(chart: HoroscopeChart, selectedBranch: EarthlyBranch, onSelect: (EarthlyBranch) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val labels = remember(context) { HoroscopeText(context) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton({ zoom = (zoom - .5f).coerceAtLeast(1f); pan = Offset.Zero }, enabled = zoom > 1f) {
                Icon(Icons.Outlined.Remove, stringResource(R.string.hs_zoom_out))
            }
            Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            IconButton({ zoom = (zoom + .5f).coerceAtMost(3f) }, enabled = zoom < 3f) {
                Icon(Icons.Outlined.Add, stringResource(R.string.hs_zoom_in))
            }
            TextButton({ zoom = 1f; pan = Offset.Zero }) { Text(stringResource(R.string.hs_fit)) }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f).clipToBounds().testTag("hs_chart_grid")) {
            val cell = maxWidth / 4
            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { _, delta, scale, _ ->
                    zoom = (zoom * scale).coerceIn(1f, 3f)
                    val bound = size.width * (zoom - 1f) / 2
                    pan = Offset((pan.x + delta.x).coerceIn(-bound, bound), (pan.y + delta.y).coerceIn(-bound, bound))
                }
            }.graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y }) {
                chart.palaces.forEach { palace ->
                    val (col, row) = chartCell(palace.branch)
                    val active = palace.branch == selectedBranch
                    val background by animateColorAsState(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        animationSpec=vn.lichviet.app.ui.appMotionSpec(), label = "palaceSelection")
                    Surface(color = background, modifier = Modifier.offset(cell * col, cell * row).size(cell)
                        .border(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        .clickable(role = Role.Button) { onSelect(palace.branch) }
                        .semantics { selected = active; contentDescription = labels.palace(palace) }
                        .testTag("hs_palace_${palace.branch.name}")) {
                        Column(Modifier.padding(5.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(labels.palaces[palace.role.ordinal], fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(palace.mainStars.joinToString(" · ", transform = labels::star).ifEmpty { stringResource(R.string.hs_no_main_short) },
                                fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.secondary)
                            Text(labels.branches[palace.branch.ordinal], fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Column(Modifier.offset(cell, cell).size(cell * 2).padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.hs_chart_center), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(labels.bureaus[chart.bureau.ordinal], Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.hs_life_body, labels.branches[chart.lifeBranch.ordinal], labels.branches[chart.bodyBranch.ordinal]),
                        Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text(stringResource(R.string.hs_gestures), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun HoroscopePalaceDetail(chart: HoroscopeChart, palace: HoroscopePalace) {
    val context = LocalContext.current
    val labels = remember(context) { HoroscopeText(context) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("hs_palace_detail")) {
        Text(labels.palace(palace), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.hs_palace_context, labels.topics[palace.role.ordinal]), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.hs_main_stars), style = MaterialTheme.typography.labelLarge)
        Text(palace.mainStars.joinToString(" · ", transform = labels::star).ifEmpty { stringResource(R.string.hs_no_stars) })
        Text(stringResource(R.string.hs_aux_stars), style = MaterialTheme.typography.labelLarge)
        Text(palace.auxiliaryStars.joinToString(" · ", transform = labels::star).ifEmpty { stringResource(R.string.hs_no_stars) })
        Text(stringResource(R.string.hs_major, palace.majorCycle.firstVirtualAge, palace.majorCycle.lastVirtualAge))
        Text(stringResource(R.string.hs_major_note), style = MaterialTheme.typography.bodySmall)
        Text("安十二宮例 · 安大限訣 · ${chart.ruleSetId}", style = MaterialTheme.typography.labelSmall)
    }
}
