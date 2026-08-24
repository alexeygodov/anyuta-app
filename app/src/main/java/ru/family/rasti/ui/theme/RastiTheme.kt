package ru.family.rasti.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Leaf = Color(0xFF557563)
val LeafDark = Color(0xFF294C3A)
val Cream = Color(0xFFF8F5EE)
val Peach = Color(0xFFEAB69B)
val Sky = Color(0xFFA9C9D5)
val Ink = Color(0xFF26342D)

private val lightColors = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8DA),
    onPrimaryContainer = LeafDark,
    secondary = Color(0xFF91634E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCA),
    onSecondaryContainer = Color(0xFF371205),
    tertiary = Color(0xFF386A7A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1E8F5),
    onTertiaryContainer = Color(0xFF001F28),
    background = Cream,
    onBackground = Ink,
    surface = Color(0xFFFFFCF7),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7E9E2),
    onSurfaceVariant = Color(0xFF434943),
    outline = Color(0xFF747C76),
    outlineVariant = Color(0xFFC3C9C2),
    error = Color(0xFFBA1A1A),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFAFCFB9),
    onPrimary = Color(0xFF173729),
    primaryContainer = Color(0xFF365848),
    onPrimaryContainer = Color(0xFFCBEBD5),
    secondary = Color(0xFFFFB69A),
    onSecondary = Color(0xFF55200E),
    secondaryContainer = Color(0xFF713823),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Color(0xFFA5CDDB),
    onTertiary = Color(0xFF073641),
    tertiaryContainer = Color(0xFF28505B),
    onTertiaryContainer = Color(0xFFC1E9F6),
    background = Color(0xFF101512),
    onBackground = Color(0xFFE0E4DE),
    surface = Color(0xFF161B18),
    onSurface = Color(0xFFE0E4DE),
    surfaceVariant = Color(0xFF3F4942),
    onSurfaceVariant = Color(0xFFBEC9C0),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF3F4942),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun RastiTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) darkColors else lightColors, content = content)
}
