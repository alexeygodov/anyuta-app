package ru.family.rasti.ui

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.family.rasti.ui.theme.darkColors
import ru.family.rasti.ui.theme.lightColors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThemeContrastTest {
    @Test fun primaryTextPairsAndTranslucentBottleMeetContrastFloor() {
        listOf(lightColors, darkColors).forEach { colors ->
            val pairs = listOf(
                colors.onBackground to colors.background,
                colors.onSurface to colors.surface,
                colors.onSurface to colors.surfaceContainerHigh,
                colors.onSurfaceVariant to colors.surfaceContainerHigh,
                colors.onSurface to colors.surfaceContainerLow,
                colors.onPrimary to colors.primary,
                colors.onPrimaryContainer to colors.primaryContainer,
                colors.onSecondary to colors.secondary,
                colors.onSecondaryContainer to colors.secondaryContainer,
                colors.onTertiaryContainer to colors.tertiaryContainer,
                colors.onErrorContainer to colors.errorContainer,
                colors.onSurface to colors.surface.copy(alpha = .55f).compositeOver(colors.secondaryContainer),
            )
            pairs.forEach { (text, background) ->
                val contrast = ColorUtils.calculateContrast(text.toArgb(), background.toArgb())
                assertTrue("Contrast $contrast below 4.5: $text on $background", contrast >= 4.5)
            }
        }
    }
}
