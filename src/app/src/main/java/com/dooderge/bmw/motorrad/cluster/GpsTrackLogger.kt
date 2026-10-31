package com.dooderge.bmw.motorrad.cluster

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** GPS 트랙 CSV — Downloads/cluster/gps, KST 기준 하루 1파일 */
class GpsTrackLogger(private val context: Context) {
    private val lock = Any()
    private var writer: BufferedWriter? = null
    private var fileDate: LocalDate? = null

    /** 한 포인트를 CSV 한 줄로 기록. 날짜가 바뀌면 파일을 다시 연다. */
    fun append(
        time: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        speedKmh: Int,
        rpm: Int,
    ) {
        synchronized(lock) {
            val kst = time.withZoneSameInstant(KST)
            val day = kst.toLocalDate()
            if (writer == null || fileDate != day) {
                if (!openDayFileLocked(day)) return
            }
            val writer = writer ?: return
            writer.write(
                String.format(
                    Locale.US,
                    "%s,%.6f,%.6f,%.1f,%d,%d",
                    kst.format(LINE_TIME),
                    latitude,
                    longitude,
                    altitude,
                    speedKmh,
                    rpm
                )
            )
            writer.newLine()
            writer.flush()
        }
    }

    /** 열려 있는 writer를 닫고 날짜 상태를 초기한다. */
    fun release() {
        synchronized(lock) {
            closeWriterLocked()
            fileDate = null
        }
    }

    /** day에 해당하는 gps_yyyyMMdd.csv 를 append/create 한다. */
    private fun openDayFileLocked(day: LocalDate): Boolean {
        val name = "gps_" + day.format(DAY_FILE) + ".csv"
        val existing = DownloadsMediaStore.findByDisplayName(context, name, RELATIVE_DIR)
        val uri: Uri
        val append: Boolean
        if (existing != null) {
            uri = existing
            append = true
        } else {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_DIR)
            }
            uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: return false
            append = false
        }
        val mode = if (append) "wa" else "w"
        val stream = context.contentResolver.openOutputStream(uri, mode) ?: return false
        val next = OutputStreamWriter(stream, StandardCharsets.UTF_8).buffered()
        closeWriterLocked()
        writer = next
        fileDate = day
        if (!append) {
            next.write("time,latitude,longitude,altitude,speed,rpm")
            next.newLine()
            next.flush()
        }
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
        val RELATIVE_DIR: String =
            "${Environment.DIRECTORY_DOWNLOADS}/cluster/gps"
        private val KST = java.time.ZoneId.of("Asia/Seoul")
        private val LINE_TIME: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private val DAY_FILE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
