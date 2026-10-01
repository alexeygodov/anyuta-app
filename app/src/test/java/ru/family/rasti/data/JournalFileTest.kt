package ru.family.rasti.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class JournalFileTest {
    @get:Rule val temporary = TemporaryFolder()
    private val first = AppData(profile = ChildProfile(name = "Тест", birthDate = "2026-01-01"))
    private val second = first.copy(days = mapOf("2026-09-12" to DayRecord("2026-09-12", note = "Новая запись")))

    @Test fun firstLaunchIsEmptyAndDoesNotCreateFiles() {
        val journal = JournalFile(temporary.root)
        assertTrue(journal.read().days.isEmpty())
        assertTrue(temporary.root.listFiles().orEmpty().isEmpty())
        assertFalse(journal.wasRecovered())
    }

    @Test fun damagedPrimaryRecoversPreviousCopyAndPreservesOriginal() {
        val journal = JournalFile(temporary.root)
        journal.write(first)
        journal.write(second)
        File(temporary.root, "rasti-data.json").writeText("{broken")
        assertEquals(first, journal.read())
        assertTrue(journal.wasRecovered())
        val damaged = temporary.root.listFiles()!!.single { it.name.startsWith("rasti-data.corrupt-") }
        assertEquals("{broken", damaged.readText())
        assertEquals(first, JournalFile(temporary.root).read())
        journal.acknowledgeRecovery()
        assertFalse(journal.wasRecovered())
    }

    @Test fun unchangedWritesDoNotReplacePreviousBackup() {
        val journal = JournalFile(temporary.root)
        journal.write(first)
        journal.write(second)
        journal.write(second)
        assertEquals(first, JsonCodec.decodeAppData(File(temporary.root, "rasti-data.backup.json").readText()))
        assertEquals(second, journal.read())
    }

    @Test fun unreadableFilesBlockWritesInsteadOfCreatingEmptyDiary() {
        val original = File(temporary.root, "rasti-data.json").apply { writeText("{broken") }
        val backup = File(temporary.root, "rasti-data.backup.json").apply { writeText("also broken") }
        val journal = JournalFile(temporary.root)
        assertThrows(JournalReadException::class.java) { journal.read() }
        assertThrows(JournalReadException::class.java) { journal.write(first) }
        assertEquals("{broken", original.readText())
        assertEquals("also broken", backup.readText())
    }

    @Test fun validJsonWithMissingDiaryStructureIsNotAnEmptyDiary() {
        File(temporary.root, "rasti-data.json").writeText("{}")
        assertThrows(JournalReadException::class.java) { JournalFile(temporary.root).read() }
    }

    @Test fun interruptedAtomicWriteRestoresLastCommittedData() {
        val journal = JournalFile(temporary.root)
        journal.write(first)
        journal.write(second)
        val primary = File(temporary.root, "rasti-data.json")
        // API 28: a process stopped after startWrite leaves the committed file in .bak.
        primary.copyTo(File(temporary.root, "rasti-data.json.bak"))
        primary.writeText("{partial write")
        assertEquals(second, JournalFile(temporary.root).read())
        assertFalse(journal.wasRecovered())
    }

    @Test fun missingPrimaryUsesBackupAndRequestsAcknowledgement() {
        val journal = JournalFile(temporary.root)
        journal.write(first)
        assertTrue(File(temporary.root, "rasti-data.json").delete())
        assertEquals(first, journal.read())
        assertTrue(journal.wasRecovered())
    }
}
