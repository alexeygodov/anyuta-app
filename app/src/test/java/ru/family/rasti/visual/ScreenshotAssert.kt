package ru.family.rasti.visual

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import kotlin.math.abs

internal fun assertScreenshot(name: String, actual: Bitmap) {
    val root = File(requireNotNull(System.getProperty("anyuta.screenshots.root")))
    val output = File(requireNotNull(System.getProperty("anyuta.screenshots.output")))
    output.mkdirs()
    val actualFile = File(output, "$name-actual.png")
    actualFile.outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val expectedFile = File(root, "$name.png")
    if (System.getProperty("anyuta.screenshots.record") == "true") {
        check(System.getenv("CI") != "true") { "Recording screenshot baselines in CI is forbidden" }
        root.mkdirs()
        actualFile.copyTo(expectedFile, overwrite = true)
        return
    }
    assertTrue("Missing baseline: $expectedFile; review and record locally", expectedFile.exists())
    val expected = requireNotNull(BitmapFactory.decodeFile(expectedFile.absolutePath))
    assertEquals("Screenshot width: $name", expected.width, actual.width)
    assertEquals("Screenshot height: $name", expected.height, actual.height)
    val diff = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
    var changed = 0
    for (y in 0 until actual.height) for (x in 0 until actual.width) {
        val a = actual.getPixel(x, y)
        val b = expected.getPixel(x, y)
        val differs = listOf(0, 8, 16, 24).any { shift ->
            abs(((a ushr shift) and 255) - ((b ushr shift) and 255)) > 3
        }
        if (differs) changed++
        diff.setPixel(x, y, if (differs) 0xffff00ff.toInt() else 0xffeeeeee.toInt())
    }
    val fraction = changed.toDouble() / (actual.width * actual.height)
    if (fraction > .001) {
        File(output, "$name-diff.png").outputStream().use { diff.compress(Bitmap.CompressFormat.PNG, 100, it) }
        expectedFile.copyTo(File(output, "$name-expected.png"), overwrite = true)
    }
    assertTrue("Screenshot changed: $name (${changed} pixels). Inspect $output", fraction <= .001)
}
