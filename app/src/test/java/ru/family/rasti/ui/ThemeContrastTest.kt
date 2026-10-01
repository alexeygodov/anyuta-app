package ru.family.rasti.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    @Test fun actualTextPairsAndTranslucentFillsMeetContrastFloor() {
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
                colors.onSurface to colors.surface,
                colors.onTertiary to colors.tertiary,
                colors.onError to colors.error,
                colors.inverseOnSurface to colors.inverseSurface,
                colors.inversePrimary to colors.inverseSurface,
                colors.onPrimaryContainer to colors.primaryContainer,
            )
            val neutrals = listOf(colors.background, colors.surface, colors.surfaceDim, colors.surfaceBright,
                colors.surfaceContainerLowest, colors.surfaceContainerLow, colors.surfaceContainer,
                colors.surfaceContainerHigh, colors.surfaceContainerHighest, colors.surfaceVariant)
            val actualPairs = pairs + neutrals.flatMap { listOf(colors.onSurface to it, colors.onSurfaceVariant to it) } +
                listOf(colors.surface, colors.surfaceContainerLow).flatMap { surface ->
                    listOf(colors.primary, colors.secondary, colors.tertiary, colors.error).map { it to surface }
                }
            val bottleBackgrounds = listOf(colors.surfaceVariant, colors.secondaryContainer).map { beneath ->
                colors.surface.copy(alpha = .55f).compositeOver(beneath)
            }
            val summaryBackground = colors.primary.copy(alpha = .16f).compositeOver(colors.surface)
            val compositedPairs = actualPairs + bottleBackgrounds.flatMap { background ->
                listOf(colors.onSurface to background, colors.onSurfaceVariant to background)
            } + listOf(colors.onSurface to summaryBackground, colors.onSurfaceVariant to summaryBackground)
            compositedPairs.forEach { (text, background) ->
                val contrast = ColorUtils.calculateContrast(text.toArgb(), background.toArgb())
                assertTrue("Contrast $contrast below 4.5: $text on $background", contrast >= 4.5)
            }
        }
    }
    @Test fun vitaminTextIsReadableInBothModesAndEveryPulsePhase() {
        listOf(lightColors, darkColors).forEach { colors ->
            listOf(false, true).forEach { shouldPulse ->
                listOf(0f, .25f, .5f, .75f, 1f).forEach { phase ->
                    val borderAlpha = if (shouldPulse) .45f + .55f * phase else 1f
                    val background = lerp(colors.errorContainer, colors.error, if (shouldPulse) .18f * phase else 0f)
                        .compositeOver(colors.background)
                    val border = colors.error.copy(alpha = borderAlpha).compositeOver(background)
                    assertTrue(border.alpha == 1f)
                    val foreground = colors.onErrorContainer.compositeOver(background)
                    assertTrue(ColorUtils.calculateContrast(foreground.toArgb(), background.toArgb()) >= 4.5)
                }
            }
        }
        // Demonstrate the original regression so reverting the pair cannot look harmless.
        val oldBackground = lerp(lightColors.error, lightColors.errorContainer, .38f)
        assertTrue(ColorUtils.calculateContrast(Color.White.toArgb(), oldBackground.toArgb()) < 4.5)
    }

    @Test fun significantOutlinesAndMarkersMeetThreeToOne() {
        listOf(lightColors, darkColors).forEach { colors ->
            listOf(colors.surface, colors.surfaceContainerLow).forEach { background ->
                listOf(colors.outline, colors.primary, colors.secondary, colors.tertiary).forEach { foreground ->
                    assertTrue(ColorUtils.calculateContrast(foreground.toArgb(), background.toArgb()) >= 3.0)
                }
            }
        }
    }

}
