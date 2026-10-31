package com.dooderge.bmw.motorrad.cluster

import com.dooderge.bmw.motorrad.cluster.ui.component.applyClusterImmersiveMode

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterFixedFontScale
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterSideActionButton
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterTwoPaneScaffold
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterActionButtonContainerColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterSettingsColorScheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogKind {
    GPS,
    LIN;

    val title: String
        get() = when (this) {
            GPS -> "GPS Logs"
            LIN -> "LIN Logs"
        }

    val relativeDir: String
        get() = when (this) {
            GPS -> GpsTrackLogger.RELATIVE_DIR
            LIN -> LinDataLogger.RELATIVE_DIR
        }

    val pathHint: String
        get() = when (this) {
            GPS -> "Downloads/cluster/gps"
            LIN -> "Downloads/cluster/lin"
        }

    val nameLike: String
        get() = when (this) {
            GPS -> "gps_%.csv"
            LIN -> "%.txt"
        }
}

data class LogFileEntry(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateModifiedMillis: Long,
)

class LogListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { LogKind.valueOf(it) }.getOrNull() }
            ?: LogKind.GPS
        val logs = loadLogs(kind)
        setContent {
            ClusterFixedFontScale {
                MaterialTheme(colorScheme = ClusterSettingsColorScheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        LogListScreen(
                            kind = kind,
                            logs = logs,
                            onBack = { finish() },
                            onUpload = { /* TODO: upload */ }
                        )
                    }
                }
            }
        }
    }

    private fun loadLogs(kind: LogKind): List<LogFileEntry> {
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.DATE_MODIFIED
        )
        val selection =
            "(${MediaStore.Downloads.RELATIVE_PATH}=? OR ${MediaStore.Downloads.RELATIVE_PATH}=?) AND " +
                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
        val args = arrayOf(kind.relativeDir, "${kind.relativeDir}/", kind.nameLike)
        val sort = "${MediaStore.Downloads.DISPLAY_NAME} DESC"
        val result = mutableListOf<LogFileEntry>()
        contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            sort
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                result.add(
                    LogFileEntry(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            id
                        ),
                        displayName = cursor.getString(nameCol).orEmpty(),
                        sizeBytes = cursor.getLong(sizeCol),
                        dateModifiedMillis = cursor.getLong(dateCol) * 1000L
                    )
                )
            }
        }
        return result
    }

    override fun onResume() {
        super.onResume()
        window.applyClusterImmersiveMode()
    }

    companion object {
        const val EXTRA_KIND = "log_kind"

        fun intent(context: Context, kind: LogKind): Intent {
            return Intent(context, LogListActivity::class.java).putExtra(EXTRA_KIND, kind.name)
        }
    }
}

@Composable
private fun LogListScreen(
    kind: LogKind,
    logs: List<LogFileEntry>,
    onBack: () -> Unit,
    onUpload: (LogFileEntry) -> Unit,
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = logs.firstOrNull { it.id == selectedId }
    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    ClusterTwoPaneScaffold(
        title = kind.title,
        subtitle = kind.pathHint,
        content = {
            if (logs.isEmpty()) {
                Text(
                    "No log files found.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 16.sp
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs, key = { it.id }) { entry ->
                        val selectedRow = entry.id == selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (selectedRow) 2.dp else 1.dp,
                                    color = if (selectedRow) {
                                        ClusterActionButtonContainerColor
                                    } else {
                                        Color(0xFF444444)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedId = entry.id }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.displayName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${formatSize(entry.sizeBytes)}  ·  ${
                                        dateFormat.format(Date(entry.dateModifiedMillis))
                                    }",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        sideActions = {
            ClusterSideActionButton(
                label = "Upload",
                modifier = Modifier.weight(1f),
                enabled = selected != null,
                onClick = { selected?.let(onUpload) }
            )
            ClusterSideActionButton(
                label = "Back",
                modifier = Modifier.weight(1f),
                onClick = onBack
            )
        }
    )
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.2f MB", kb / 1024.0)
}
