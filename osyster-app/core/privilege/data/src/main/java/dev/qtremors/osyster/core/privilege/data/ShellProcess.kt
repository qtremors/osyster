package dev.qtremors.osyster.core.privilege.data

import dev.qtremors.osyster.core.privilege.domain.ShellResult
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Starts one process and collects its output. Both streams are drained on their own threads so a
 * chatty command cannot fill a pipe and hang; anything still running after [TIMEOUT_SECONDS] is
 * killed. Output is capped, since some commands (dumpsys) can print megabytes.
 */
internal object ShellProcess {
    private const val TIMEOUT_SECONDS = 30L
    private const val MAX_OUTPUT = 512 * 1024

    fun run(argv: List<String>): Array<String> {
        val process = try {
            ProcessBuilder(argv).start()
        } catch (error: IOException) {
            return arrayOf("-1", "", error.message.orEmpty())
        }
        val output = StringBuilder()
        val error = StringBuilder()
        val readers = listOf(drain(process.inputStream, output), drain(process.errorStream, error))
        val finished = try {
            process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!finished) process.destroyForcibly()
        readers.forEach { it.join(JOIN_MILLIS) }
        val exit = if (finished) process.exitValue() else -1
        return arrayOf(exit.toString(), synchronized(output) { output.toString() }, synchronized(error) { error.toString() })
    }

    private fun drain(stream: InputStream, into: StringBuilder): Thread = thread(isDaemon = true, name = "shell-reader") {
        runCatching {
            stream.bufferedReader().use { reader ->
                val buffer = CharArray(BUFFER)
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    synchronized(into) {
                        if (into.length < MAX_OUTPUT) into.append(buffer, 0, minOf(read, MAX_OUTPUT - into.length))
                    }
                }
            }
        }
    }

    private const val BUFFER = 4096
    private const val JOIN_MILLIS = 1_000L
}

internal fun Array<String>.toShellResult(): ShellResult = ShellResult(
    exitCode = getOrNull(0)?.toIntOrNull() ?: -1,
    output = getOrNull(1).orEmpty(),
    error = getOrNull(2).orEmpty()
)
