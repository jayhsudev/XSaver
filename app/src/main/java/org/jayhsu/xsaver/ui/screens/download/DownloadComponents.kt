package org.jayhsu.xsaver.ui.screens.download

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.jayhsu.xsaver.R
import org.jayhsu.xsaver.ui.designsystem.Dimens
import org.jayhsu.xsaver.data.model.DownloadTask
import org.jayhsu.xsaver.data.model.DownloadStatus

// NOTE: This file groups extracted smaller composables from the oversized DownloadScreen.
// They will be incrementally adopted; some placeholders keep parameters minimal now.

@Composable
fun InlineErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(Dimens.Space8), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(Dimens.Space8))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("X") }
    }
}

@Composable
fun EmptyState(onHistory: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Link, modifier = Modifier.padding(Dimens.Space16), contentDescription = null)
        Text(
            text = stringResource(R.string.paste_link_hint),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(Dimens.Space16),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.paste_link_cta),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.Space16))
        Button(onClick = onHistory) { Text(stringResource(R.string.view_all_history)) }
    }
}

@Composable
fun DownloadTasksPanel(tasks: List<DownloadTask>, onPause: (String) -> Unit, onResume: (String) -> Unit, onCancel: (String) -> Unit) {
    if (tasks.none { it.status != DownloadStatus.Completed && it.status != DownloadStatus.Canceled }) return
    Column(modifier = Modifier.fillMaxWidth().padding(Dimens.Space8)) {
        Text(text = stringResource(R.string.downloading_tasks), style = MaterialTheme.typography.titleSmall)
        tasks.forEach { task ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space4), verticalAlignment = Alignment.CenterVertically) {
                val pct = (task.progress * 100).toInt()
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    LinearProgressIndicator(progress = { task.progress }, modifier = Modifier.fillMaxWidth().padding(top = Dimens.Space4))
                    val errSuffix = if (task.status == DownloadStatus.Error) {
                        val t = task.errorType ?: "Err"
                        val c = task.errorCode?.let { " $it" } ?: ""
                        "  ${t}${c}"
                    } else ""
                    Text("${pct}%  ${task.status}${errSuffix}", style = MaterialTheme.typography.labelSmall, color = if (task.status == DownloadStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(Dimens.Space8))
                when (task.status) {
                    DownloadStatus.Downloading -> IconButton(onClick = { onPause(task.id) }) { Icon(Icons.Filled.Close, contentDescription = "pause") }
                    DownloadStatus.Paused -> IconButton(onClick = { onResume(task.id) }) { Icon(Icons.Filled.Refresh, contentDescription = "resume") }
                    DownloadStatus.Error -> IconButton(onClick = { onResume(task.id) }) { Icon(Icons.Filled.Refresh, contentDescription = "retry") }
                    DownloadStatus.Pending -> IconButton(onClick = { /* pending no-op */ }) { Icon(Icons.Filled.Refresh, contentDescription = "pending") }
                    DownloadStatus.Completed, DownloadStatus.Canceled -> { }
                }
                if (task.status != DownloadStatus.Completed && task.status != DownloadStatus.Canceled) {
                    IconButton(onClick = { onCancel(task.id) }) { Icon(Icons.Filled.Close, contentDescription = "cancel") }
                }
            }
        }
    }
}
