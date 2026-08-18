package ru.family.rasti.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Leaf = Color(0xFF557563)
val LeafDark = Color(0xFF294C3A)
val Cream = Color(0xFFF8F5EE)
val Peach = Color(0xFFEAB69B)
val Sky = Color(0xFFA9C9D5)
val Ink = Color(0xFF26342D)

private val colors = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8DA),
    onPrimaryContainer = LeafDark,
    secondary = Color(0xFF91634E),
    secondaryContainer = Color(0xFFFFDBCA),
    background = Cream,
    onBackground = Ink,
    surface = Color(0xFFFFFCF7),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7E9E2),
    outline = Color(0xFF747C76),
    error = Color(0xFFBA1A1A),
)

@Composable
fun RastiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

