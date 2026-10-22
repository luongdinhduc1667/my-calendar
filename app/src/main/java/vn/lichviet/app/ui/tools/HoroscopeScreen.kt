package vn.lichviet.app.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vn.lichviet.core.VietnameseCalendar
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

private val branches = listOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")
private val animals = listOf("Chuột", "Trâu", "Hổ", "Mèo", "Rồng", "Rắn", "Ngựa", "Dê", "Khỉ", "Gà", "Chó", "Lợn")

@Composable
fun HoroscopeScreen() {
    val today = remember { LocalDate.now(VietnameseCalendar.zoneId) }
    var birthday by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var dateDialog by remember { mutableStateOf(false) }
    val lunar = remember(birthday) { birthday?.let(VietnameseCalendar::solarToLunar) }
    val zodiacIndex = lunar?.let { Math.floorMod(it.year - 4, 12) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ToolHeading("Góc văn hóa phương Đông", "Khám phá tuổi của bạn", "Tra con giáp và Can Chi từ ngày sinh dương lịch.")
        ToolCard {
            ZodiacWheel(selectedIndex = zodiacIndex, yearLabel = lunar?.let { VietnameseCalendar.yearCanChi(it.year) })
            DateChoice("Ngày sinh dương lịch", birthday, onClick = { dateDialog = true })
        }
        if (lunar != null && birthday != null && zodiacIndex != null) {
            ToolCard {
                Text("TUỔI ${branches[zodiacIndex].uppercase(VietnameseLocale)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Năm ${VietnameseCalendar.yearCanChi(lunar.year)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Biểu tượng: ${animals[zodiacIndex]}", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailLine("Ngày sinh âm lịch", "${lunar.day}/${lunar.month}${if (lunar.isLeap) " nhuận" else ""}/${lunar.year}")
                DetailLine("Can Chi ngày sinh", VietnameseCalendar.dayCanChi(birthday!!))
                DetailLine("Can Chi tháng sinh", VietnameseCalendar.monthCanChi(lunar))
                DetailLine("Vị trí trong vòng 12 con giáp", "${zodiacIndex + 1}/12 · Chi ${branches[zodiacIndex]}")
            }
        }
        ToolCard {
            Text("Vòng thời gian 60 năm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Mười Thiên Can kết hợp cùng mười hai Địa Chi tạo thành chu kỳ 60 năm. Trong văn hóa Việt, mỗi Địa Chi gắn với một con vật; riêng chi Mão được biểu trưng bằng con mèo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Năm âm lịch chuyển sang năm mới vào Tết Nguyên Đán. Vì vậy, ngày sinh trước Tết có thể thuộc con giáp của năm âm lịch trước, dù năm dương lịch đã thay đổi.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Đây là tra cứu lịch và biểu tượng truyền thống. Con giáp không xác định tính cách hay tương lai của một người.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = {
                uriHandler.openUri("https://dsvh.gov.vn/Upload/files/Tap%20chi%20DSVH/So%2022/2202_Muoi%20hai%20con%20giap%20trong%20van%20hoa%20cua%20nguoi%20Viet.pdf")
            }) { Text("Đọc thêm · Tạp chí Di sản văn hóa") }
        }
    }
    if (dateDialog) VietnameseDateDialog(
        initialDate = birthday ?: LocalDate.of(1995, 1, 1),
        onDismiss = { dateDialog = false },
        onSelected = { birthday = it; dateDialog = false },
        latestDate = today,
    )
}

@Composable
private fun ZodiacWheel(selectedIndex: Int?, yearLabel: String?) {
    val color = MaterialTheme.colorScheme.tertiary
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, 300.dp)
        Box(modifier = Modifier.size(side), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().clearAndSetSemantics { }) {
                val radius = size.minDimension / 2
                drawCircle(color.copy(alpha = 0.5f), radius * 0.98f, style = Stroke(1.dp.toPx()))
                drawCircle(color.copy(alpha = 0.3f), radius * 0.69f, style = Stroke(1.dp.toPx()))
                repeat(60) { index ->
                    val angle = Math.toRadians(index * 6.0 - 90)
                    val inner = radius * if (index % 5 == 0) 0.9f else 0.94f
                    val outer = radius * 0.98f
                    drawLine(
                        color.copy(alpha = if (index % 5 == 0) 0.8f else 0.4f),
                        androidx.compose.ui.geometry.Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                        androidx.compose.ui.geometry.Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            branches.forEachIndexed { index, branch ->
                val angle = Math.toRadians(index * 30.0 - 90)
                Text(
                    branch,
                    modifier = Modifier.offset(x = side * (cos(angle).toFloat() * 0.405f), y = side * (sin(angle).toFloat() * 0.405f)),
                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (index == selectedIndex) FontWeight.ExtraBold else FontWeight.Normal,
                )
            }
            Surface(
                modifier = Modifier.size(side * 0.61f).border(1.dp, color.copy(alpha = 0.3f), CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                    Text(yearLabel ?: "12 con giáp", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text("LỊCH VIỆT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}
