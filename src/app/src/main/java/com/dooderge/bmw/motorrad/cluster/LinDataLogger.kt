package com.dooderge.bmw.motorrad.cluster

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LinDataLogger(private val context: Context) {
    private val lock = Any()
    private var writer: BufferedWriter? = null
    private var lineCount = 0
    private val fileNames = mutableListOf<String>()
    var active = false
        private set

    fun start(): Boolean {
        synchronized(lock) {
            if (active) return true
            if (!openFileLocked(LocalDateTime.now())) return false
            active = true
            SettingPreference.setLinLogging(context, true)
            return true
        }
    }

    fun stop(): List<String> {
        synchronized(lock) {
            closeWriterLocked()
            active = false
            SettingPreference.setLinLogging(context, false)
            val names = fileNames.toList()
            fileNames.clear()
            return names
        }
    }

    fun release() {
        synchronized(lock) {
            closeWriterLocked()
            active = false
        }
    }

    fun append(data: ByteArray) {
        synchronized(lock) {
            if (!active) return
            if (lineCount >= MAX_LINES && !openFileLocked(LocalDateTime.now())) return
            val writer = writer ?: return
            val time = LocalDateTime.now().format(LINE_TIME)
            val bytes = data.joinToString(" ") { "0x%02X".format(it) }
            writer.write("$time $bytes")
            writer.newLine()
            writer.flush()
            lineCount++
        }
    }

    private fun openFileLocked(startedAt: LocalDateTime): Boolean {
        val name = startedAt.format(FILE_TIME) + ".txt"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/bmw-ww-log"
            )
        }
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        ) ?: return false
        val stream = context.contentResolver.openOutputStream(uri) ?: return false
        val next = OutputStreamWriter(stream, StandardCharsets.UTF_8).buffered()
        closeWriterLocked()
        writer = next
        lineCount = 0
        fileNames.add(name)
        return true
    }

    private fun closeWriterLocked() {
        try {
            writer?.flush()
            writer?.close()
        } catch (_: Exception) {
        }
        writer = null
    }

    companion object {
        const val MAX_LINES = 100_000
        private val LINE_TIME: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private val FILE_TIME: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
