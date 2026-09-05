package ru.family.rasti.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Leaf = Color(0xFF356B59)
val LeafDark = Color(0xFF183E32)
val Cream = Color(0xFFF5F7F2)
val Peach = Color(0xFFE2A477)
val Sky = Color(0xFF91ACD8)
val Ink = Color(0xFF1D2B26)

private val lightColors = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEFE5),
    onPrimaryContainer = LeafDark,
    secondary = Color(0xFF9A6545),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1CF),
    onSecondaryContainer = Color(0xFF371205),
    tertiary = Color(0xFF536B9D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE5FF),
    onTertiaryContainer = Color(0xFF243657),
    background = Cream,
    onBackground = Ink,
    surface = Color(0xFFFEFFFC),
    surfaceDim = Color(0xFFDCE3DC),
    surfaceBright = Color(0xFFFEFFFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4EE),
    surfaceContainer = Color(0xFFE9EFE8),
    surfaceContainerHigh = Color(0xFFE3EAE2),
    surfaceContainerHighest = Color(0xFFDDE5DD),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9EEE9),
    onSurfaceVariant = Color(0xFF434943),
    outline = Color(0xFF747C76),
    outlineVariant = Color(0xFFC3C9C2),
    error = Color(0xFFBA1A1A),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFA7D8BD),
    onPrimary = Color(0xFF173729),
    primaryContainer = Color(0xFF285040),
    onPrimaryContainer = Color(0xFFCBEBD5),
    secondary = Color(0xFFFFB69A),
    onSecondary = Color(0xFF55200E),
    secondaryContainer = Color(0xFF49342B),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Color(0xFFB4C5FA),
    onTertiary = Color(0xFF162B50),
    tertiaryContainer = Color(0xFF25344C),
    onTertiaryContainer = Color(0xFFDCE5FF),
    background = Color(0xFF101613),
    onBackground = Color(0xFFE0E4DE),
    surface = Color(0xFF151E1A),
    surfaceDim = Color(0xFF101613),
    surfaceBright = Color(0xFF343D38),
    surfaceContainerLowest = Color(0xFF0B110E),
    surfaceContainerLow = Color(0xFF17201B),
    surfaceContainer = Color(0xFF1D2721),
    surfaceContainerHigh = Color(0xFF253029),
    surfaceContainerHighest = Color(0xFF303B34),
    onSurface = Color(0xFFE0E4DE),
    surfaceVariant = Color(0xFF303B34),
    onSurfaceVariant = Color(0xFFBEC9C0),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF3F4942),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF542A2B),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val appShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val appTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun RastiTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = appTypography,
        shapes = appShapes,
        content = content,
    )
}
