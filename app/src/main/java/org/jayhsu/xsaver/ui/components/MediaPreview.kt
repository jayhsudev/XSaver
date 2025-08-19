package org.jayhsu.xsaver.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreview(
    mediaItem: MediaItem,
    onDownloadClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onShareClick: () -> Unit = {},
    onOpenInXClick: () -> Unit = {},
    onShowDownloadPathClick: () -> Unit = {},
    isHistoryItem: Boolean = false
) {
    var showMenuSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { /* 点击卡片预览媒体 */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // 媒体预览图
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            AsyncImage(
                model = mediaItem.thumbnailUrl ?: mediaItem.url,
                contentDescription = mediaItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 媒体类型图标覆盖
            if (mediaItem.type == MediaType.VIDEO) {
                Icon(
                    Icons.Filled.PlayArrow,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    tint = androidx.compose.ui.graphics.Color.White,
                    contentDescription = "视频"
                )
            } else if (mediaItem.type == MediaType.AUDIO) {
                // 音频特有图标
                Icon(
                    Icons.Filled.MusicNote,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    tint = androidx.compose.ui.graphics.Color.White,
                    contentDescription = "音频"
                )
            }

            // 右上角分享按钮
            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Share,
                    tint = androidx.compose.ui.graphics.Color.White,
                    contentDescription = "分享"
                )
            }
        }

        // 媒体信息
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = mediaItem.title ?: "未命名",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = mediaItem.size?.let { "${it}MB" } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 操作按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isHistoryItem) {
                // 历史记录项显示打开按钮
                TextButton(
                    onClick = { /* 打开媒体文件 */ }
                ) {
                    Text("打开")
                }
            } else {
                // 下载项显示下载按钮
                TextButton(
                    onClick = onDownloadClick
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("下载")
                }
            }

            // 右下角菜单按钮
            IconButton(onClick = { showMenuSheet = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
            }
        }
    }

    // 底部Sheet菜单
    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextButton(
                    onClick = {
                        onOpenInXClick()
                        showMenuSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("在X上打开")
                }
                TextButton(
                    onClick = {
                        onShareClick()
                        showMenuSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("分享")
                }
                TextButton(
                    onClick = {
                        onShowDownloadPathClick()
                        showMenuSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("下载路径")
                }
                if (isHistoryItem && onDeleteClick != null) {
                    TextButton(
                        onClick = {
                            onDeleteClick()
                            showMenuSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}