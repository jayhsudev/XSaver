package org.jayhsu.xsaver.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jayhsu.xsaver.R
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import org.jayhsu.xsaver.ui.components.MediaPreview
import org.jayhsu.xsaver.ui.viewmodel.HistoryViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val viewModel: HistoryViewModel = hiltViewModel()
    val context = LocalContext.current
    val mediaHistory by viewModel.mediaHistory.collectAsState()
    var selectedMediaType by remember { mutableStateOf<MediaType?>(null) } // null表示显示全部
    var showDeleteDialog by remember { mutableStateOf(false) }
    var mediaToDelete by remember { mutableStateOf<MediaItem?>(null) }

    // 过滤媒体项
    val filteredMedia = selectedMediaType?.let { type ->
        mediaHistory.filter { it.type == type }
    } ?: mediaHistory // 如果selectedMediaType为null，显示全部

    Column(modifier = Modifier.fillMaxSize()) {
        // 分类标签
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            SegmentedButton(
                selected = selectedMediaType == null,
                onClick = { selectedMediaType = null }, // 显示全部
                icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                label = { Text("全部") }
            )
            SegmentedButton(
                selected = selectedMediaType == MediaType.VIDEO,
                onClick = { selectedMediaType = MediaType.VIDEO },
                icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = null) },
                label = { Text(stringResource(R.string.videos)) }
            )
            SegmentedButton(
                selected = selectedMediaType == MediaType.IMAGE,
                onClick = { selectedMediaType = MediaType.IMAGE },
                icon = { Icon(Icons.Filled.Image, contentDescription = null) },
                label = { Text(stringResource(R.string.images)) }
            )
            SegmentedButton(
                selected = selectedMediaType == MediaType.AUDIO,
                onClick = { selectedMediaType = MediaType.AUDIO },
                icon = { Icon(Icons.Filled.Album, contentDescription = null) },
                label = { Text(stringResource(R.string.audios)) }
            )
        }

        // 媒体列表
        if (filteredMedia.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.empty_history),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(filteredMedia) { mediaItem ->
                    MediaPreview(
                        mediaItem = mediaItem,
                        onDownloadClick = { /* 已下载，可打开或分享 */ },
                        onDeleteClick = {
                            mediaToDelete = mediaItem
                            showDeleteDialog = true
                        },
                        onShareClick = {
                            viewModel.shareMedia(mediaItem)?.let { intent ->
                                context.startActivity(intent)
                            }
                        },
                        onOpenInXClick = {
                            viewModel.openInX(mediaItem)
                        },
                        onShowDownloadPathClick = {
                            val path = viewModel.getDownloadPath(mediaItem)
                            // 显示下载路径，可以使用Toast或Dialog
                            android.widget.Toast.makeText(context, "下载路径: $path", android.widget.Toast.LENGTH_LONG).show()
                        },
                        isHistoryItem = true
                    )
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog && mediaToDelete != null) {
        AlertDialog(
            title = { Text("删除确认") },
            text = { Text("确定要删除这个媒体项吗?") },
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    mediaToDelete?.let { viewModel.deleteMedia(it) }
                    showDeleteDialog = false
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}