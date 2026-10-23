package vn.lichviet.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.lichviet.app.R
import vn.lichviet.app.data.AppTheme
import vn.lichviet.app.data.UserPreferences

/** Standalone onboarding. Permission requests are owned by the host after explicit opt-in. */
@Composable
fun OnboardingScreen(
    preferences: UserPreferences,
    language: String,
    onLanguage: (String) -> Unit,
    onTheme: (AppTheme) -> Unit,
    onMonday: (Boolean) -> Unit,
    onLunar: (Boolean) -> Unit,
    onComplete: (setupReminders: Boolean) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val animationsEnabled = LocalAppAnimations.current
    suspend fun goToPage(page: Int) {
        if (animationsEnabled) pager.animateScrollToPage(page) else pager.scrollToPage(page)
    }
    var reminders by rememberSaveable { mutableStateOf(false) }
    val titles = listOf(R.string.onboarding_welcome_title, R.string.onboarding_lunar_title,
        R.string.onboarding_events_title, R.string.onboarding_personalize_title)
    val bodies = listOf(R.string.onboarding_welcome_body, R.string.onboarding_lunar_body,
        R.string.onboarding_events_body, R.string.onboarding_personalize_body)
    val icons = listOf(Icons.Outlined.AutoAwesome, Icons.Outlined.CalendarMonth,
        Icons.Outlined.EventAvailable, Icons.Outlined.Tune)
    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { goToPage(pager.currentPage - 1) }
    }
    Surface(modifier=Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
        Column(Modifier.fillMaxHeight().widthIn(max = 840.dp).fillMaxWidth().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { onComplete(false) }) { Text(stringResource(R.string.onboarding_skip)) }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.Top) { page ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    val scale by animateFloatAsState(if (pager.currentPage == page) 1f else .86f,
                        animationSpec = appMotionSpec(), label = "onboarding_art")
                    OnboardingArt(icons[page], page, Modifier.scale(scale))
                    AnimatedContent(targetState = page, transitionSpec = {
                        if(animationsEnabled) fadeIn() togetherWith fadeOut()
                        else androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                    }, label = "onboarding_title") { index ->
                        Text(stringResource(titles[index]), style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    Text(stringResource(bodies[page]), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    if (page == 3) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.onboarding_language), style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FilterChip(language == "vi", { onLanguage("vi") }, label = { Text(stringResource(R.string.language_vi)) })
                                FilterChip(language == "en", { onLanguage("en") }, label = { Text(stringResource(R.string.language_en)) })
                            }
                            Text(stringResource(R.string.onboarding_theme), style = MaterialTheme.typography.titleSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(AppTheme.SYSTEM to R.string.theme_system, AppTheme.LIGHT to R.string.theme_light,
                                    AppTheme.DARK to R.string.theme_dark).forEach { (theme, label) ->
                                    FilterChip(preferences.theme == theme, { onTheme(theme) }, label = { Text(stringResource(label)) })
                                }
                            }
                            Text(stringResource(R.string.onboarding_week_start), style = MaterialTheme.typography.titleSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(preferences.mondayFirst, { onMonday(true) }, label = { Text(stringResource(R.string.week_start_monday)) })
                                FilterChip(!preferences.mondayFirst, { onMonday(false) }, label = { Text(stringResource(R.string.week_start_sunday)) })
                            }
                            OnboardingToggle(stringResource(R.string.onboarding_show_lunar), preferences.showLunar, onLunar)
                            OnboardingToggle(stringResource(R.string.onboarding_setup_reminders), reminders) { reminders = it }
                            AnimatedVisibility(reminders,
                                enter=if(animationsEnabled)fadeIn() else androidx.compose.animation.EnterTransition.None,
                                exit=if(animationsEnabled)fadeOut() else androidx.compose.animation.ExitTransition.None) {
                                Text(stringResource(R.string.onboarding_reminder_explanation),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(4) { page ->
                    val description = stringResource(R.string.onboarding_page, page + 1, 4)
                    IconButton(onClick = { scope.launch { goToPage(page) } },
                        modifier = Modifier.semantics { contentDescription = description }) {
                        Box(Modifier.size(if (pager.currentPage == page) 10.dp else 7.dp).background(
                            if (pager.currentPage == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (pager.currentPage > 0) TextButton(onClick = { scope.launch { goToPage(pager.currentPage - 1) } }) {
                    Text(stringResource(R.string.onboarding_back))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    if (pager.currentPage == 3) onComplete(reminders)
                    else scope.launch { goToPage(pager.currentPage + 1) }
                }) { Text(stringResource(if (pager.currentPage == 3) R.string.onboarding_start else R.string.onboarding_next)) }
            }
        }
        }
    }
}

@Composable
private fun OnboardingToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants=true){}.toggleable(checked,role=Role.Switch,onValueChange=onChange), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, null)
    }
}

@Composable
private fun OnboardingArt(icon: ImageVector, page: Int, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.tertiary
    Box(modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(gold.copy(alpha = .10f), radius = size.minDimension * .48f)
            drawCircle(gold.copy(alpha = .45f), radius = size.minDimension * .43f, style = Stroke(1.dp.toPx()))
            drawArc(accent.copy(alpha = .6f), startAngle = page * 70f, sweepAngle = 100f,
                useCenter = false, style = Stroke(3.dp.toPx()))
        }
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(58.dp))
    }
}
