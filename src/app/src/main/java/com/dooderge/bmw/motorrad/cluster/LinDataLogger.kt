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

/** LIN 프레임 텍스트 로그 — Downloads/cluster/lin, 파일당 MAX_LINES. */
class LinDataLogger(private val context: Context) {
    private val lock = Any()
    private var writer: BufferedWriter? = null
    private var lineCount = 0
    private val fileNames = mutableListOf<String>()
    private val hexScratch = StringBuilder(96)
    var active = false
        private set

    /** 로깅 시작. 이미 활성면 true. prefs는 설정 토글이 소유한다. */
    fun start(): Boolean {
        synchronized(lock) {
            if (active) return true
            if (!openFileLocked(LocalDateTime.now())) return false
            active = true
            return true
        }
    }

    /** 로깅 중지 후 이번 세션에 만든 파일명 목록을 반환. */
    fun stop(): List<String> {
        synchronized(lock) {
            closeWriterLocked()
            active = false
            val names = fileNames.toList()
            fileNames.clear()
            return names
        }
    }

    /** 앱/서비스 종료 시 writer만 닫고 prefs는 건드리지 않는다. */
    fun release() {
        synchronized(lock) {
            closeWriterLocked()
            active = false
        }
    }

    /** 한 프레임을 `time 0x.. 0x..` 형식으로 append. flush는 배치. */
    fun append(data: ByteArray) {
        synchronized(lock) {
            if (!active) return
            if (lineCount >= MAX_LINES && !openFileLocked(LocalDateTime.now())) return
            val writer = writer ?: return
            val time = LocalDateTime.now().format(LINE_TIME)
            writer.write(time)
            writer.write(' '.code)
            writer.write(formatHexLine(data))
            writer.newLine()
            lineCount++
            // 매 줄 flush는 I/O 병목 — 주기적으로만 디스크에 반영
            if (lineCount % FLUSH_EVERY_LINES == 0) {
                writer.flush()
            }
        }
    }

    /** ByteArray → "0xAA 0xBB …" (재사용 StringBuilder). */
    private fun formatHexLine(data: ByteArray): String {
        hexScratch.setLength(0)
        for (i in data.indices) {
            if (i > 0) hexScratch.append(' ')
            hexScratch.append("0x")
            val v = data[i].toInt() and 0xFF
            hexScratch.append(HEX[(v ushr 4) and 0xF])
            hexScratch.append(HEX[v and 0xF])
        }
        return hexScratch.toString()
    }

    private fun openFileLocked(startedAt: LocalDateTime): Boolean {
        val name = startedAt.format(FILE_TIME) + ".txt"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_DIR)
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
        private const val FLUSH_EVERY_LINES = 32
        private val HEX = "0123456789ABCDEF".toCharArray()
        val RELATIVE_DIR: String =
            "${Environment.DIRECTORY_DOWNLOADS}/cluster/lin"
        private val LINE_TIME: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private val FILE_TIME: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
