package vn.lichviet.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Vermilion = Color(0xFFB94332)
val Gold = Color(0xFFB28A4E)
val Forest = Color(0xFF52705C)
val LocalAppAnimations = staticCompositionLocalOf { true }

@Composable
fun <T> appMotionSpec(): FiniteAnimationSpec<T> = if (LocalAppAnimations.current) spring() else snap()
private val Light = lightColorScheme(
    primary = Vermilion, onPrimary = Color.White, primaryContainer = Color(0xFFF6E6DF),
    onPrimaryContainer = Color(0xFF873526), secondary = Forest,
    secondaryContainer = Color(0xFFE8EEE5), onSecondaryContainer = Color(0xFF34513F),
    tertiary = Gold, background = Color(0xFFF8F6F0), onBackground = Color(0xFF282D2A),
    surface = Color(0xFFFFFEFA), onSurface = Color(0xFF282D2A),
    surfaceVariant = Color(0xFFF0EDE5), onSurfaceVariant = Color(0xFF82847B),
    outline = Color(0xFFE3DFD5), outlineVariant = Color(0xFFECE8DF)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFF09A83), onPrimary = Color(0xFF4E211A),
    primaryContainer = Color(0xFF56362D), onPrimaryContainer = Color(0xFFFFDACE),
    secondary = Color(0xFFADC5A6), secondaryContainer = Color(0xFF314537),
    tertiary = Color(0xFFDEC18D), background = Color(0xFF181D1A),
    surface = Color(0xFF222824), surfaceVariant = Color(0xFF2E352F),
    onSurface = Color(0xFFF1EDE3), onBackground = Color(0xFFF1EDE3),
    onSurfaceVariant = Color(0xFFB0B4A8), outline = Color(0xFF485044), outlineVariant = Color(0xFF363E34)
)

@Composable
fun LichVietTheme(theme: String = "SYSTEM", animationsEnabled: Boolean = true, content: @Composable () -> Unit) {
    val dark = theme == "DARK" || (theme == "SYSTEM" && isSystemInDarkTheme())
    CompositionLocalProvider(LocalAppAnimations provides animationsEnabled) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = Typography(
            displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 64.sp, lineHeight = 72.sp),
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 38.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 25.sp, lineHeight = 33.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
            labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp, letterSpacing = 0.6.sp)
        ), content = content
    )
    }
}
