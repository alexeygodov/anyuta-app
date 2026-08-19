package ru.family.rasti.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {
    @Test
    fun isVersionNewer_compares_semantic_versions() {
        assertTrue(isVersionNewer("0.4.0", "0.3.2"))
        assertTrue(isVersionNewer("1.0.1", "1.0.0"))
        assertFalse(isVersionNewer("0.4.0", "0.4.0"))
        assertFalse(isVersionNewer("0.3.9", "0.4.0"))
    }
}
