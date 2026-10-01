package ru.family.rasti.data

import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

class JournalReadException(cause: Throwable) : IOException(
    "Не удалось прочитать дневник и его резервную копию. Исходные файлы сохранены.", cause,
)

internal class JournalFile(private val directory: File) {
    private val primary = File(directory, "rasti-data.json")
    private val backup = File(directory, "rasti-data.backup.json")
    private val recoveryMarker = File(directory, "rasti-data.recovered")

    fun wasRecovered(): Boolean = recoveryMarker.exists()
    fun acknowledgeRecovery() { recoveryMarker.delete() }

    fun read(): AppData = synchronized(lock) {
        if (!hasFiles(primary) && !hasFiles(backup)) return@synchronized AppData()
        try {
            decode(readAtomic(primary))
        } catch (error: Exception) {
            try {
                val raw = readAtomic(backup)
                val restored = decode(raw)
                // Do not overwrite the original if preserving it or the recovery notice fails.
                if (primary.exists()) {
                    primary.copyTo(File(directory, "rasti-data.corrupt-${UUID.randomUUID()}.json"))
                }
                writeAtomic(recoveryMarker, "previous-local-copy")
                writeAtomic(primary, raw)
                restored
            } catch (recoveryError: Exception) {
                error.addSuppressed(recoveryError)
                throw JournalReadException(error)
            }
        }
    }

    fun write(data: AppData) = synchronized(lock) {
        val raw = JsonCodec.encodeAppData(data)
        decode(raw)
        if (hasFiles(primary) || hasFiles(backup)) {
            // read() either recovers a valid file or refuses to overwrite damaged data.
            read()
            val previous = readAtomic(primary)
            if (previous == raw) return@synchronized
            writeAtomic(backup, previous)
        } else {
            writeAtomic(backup, raw)
        }
        writeAtomic(primary, raw)
    }

    private fun decode(raw: String): AppData {
        val root = JSONObject(raw)
        require(root.optJSONObject("profile") != null && root.optJSONArray("days") != null) {
            "Invalid local diary structure"
        }
        return JsonCodec.decodeAppData(raw)
    }

    private fun hasFiles(file: File): Boolean =
        file.exists() || File("${file.path}.bak").exists() || File("${file.path}.new").exists()

    private fun readAtomic(file: File): String = AtomicFile(file).openRead().bufferedReader().use { it.readText() }

    companion object {
        // LocalStore is created by the Activity, workers and widget receivers in the same process.
        private val lock = Any()

        internal fun writeAtomic(file: File, raw: String) {
            val atomic = AtomicFile(file)
            val output = atomic.startWrite()
            try {
                output.write(raw.toByteArray(Charsets.UTF_8))
                atomic.finishWrite(output)
            } catch (error: Exception) {
                atomic.failWrite(output)
                throw error
            }
        }
    }
}
