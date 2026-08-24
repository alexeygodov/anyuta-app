package ru.family.rasti.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {

    @Test
    fun stored_dark_theme_is_restored() {
        assertEquals(AppTheme.DARK, parseAppTheme("DARK"))
    }

    @Test
    fun missing_or_unknown_theme_falls_back_to_light() {
        assertEquals(AppTheme.LIGHT, parseAppTheme(null))
        assertEquals(AppTheme.LIGHT, parseAppTheme("SYSTEM"))
    }
}
