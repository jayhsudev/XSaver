package org.jayhsu.xsaver.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import androidx.compose.ui.tooling.preview.Preview
import org.jayhsu.xsaver.ui.theme.XSaverTheme

@Composable
fun MediaLane(
    mediaItem: MediaItem,
    onOpenInX: () -> Unit,
    onShare: () -> Unit,
    onShowDownloadPath: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit,
    showSelection: Boolean = false,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    var showSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable { onPreview() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // 左侧缩略图
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                when (mediaItem.type) {
                    MediaType.AUDIO -> Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> {
                        AsyncImage(
                            model = mediaItem.thumbnailUrl ?: mediaItem.url,
                            contentDescription = mediaItem.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(96.dp)
                        )
                        if (mediaItem.type == MediaType.VIDEO) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                if (showSelection) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }

            // 右侧信息与操作
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mediaItem.title ?: "未命名",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = "分享") }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = mediaItem.sourceUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { showSheet = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "更多") }
                }
            }
        }
    }

    MediaMoreVert(
        visible = showSheet,
        onDismiss = { showSheet = false },
        onOpenInX = onOpenInX,
        onShare = onShare,
        onShowDownloadPath = onShowDownloadPath,
        onDelete = onDelete
    )
}

// Previews inline per request
private fun mockLaneItem(type: MediaType) = MediaItem(
    url = when (type) {
        MediaType.IMAGE -> "https://picsum.photos/400/300"
        MediaType.VIDEO -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        MediaType.AUDIO -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    },
    title = "示例 ${type.name}",
    thumbnailUrl = null,
    type = type,
    sourceUrl = "https://x.com/post/mock"
)

@Preview
@Composable
private fun MediaLane_Image_Preview() {
    XSaverTheme {
        MediaLane(
            mediaItem = mockLaneItem(MediaType.IMAGE),
            onOpenInX = {}, onShare = {}, onShowDownloadPath = {}, onDelete = {}, onPreview = {}
        )
    }
}

@Preview
@Composable
private fun MediaLane_Video_Preview() {
    XSaverTheme {
        MediaLane(
            mediaItem = mockLaneItem(MediaType.VIDEO),
            onOpenInX = {}, onShare = {}, onShowDownloadPath = {}, onDelete = {}, onPreview = {}
        )
    }
}

@Preview
@Composable
private fun MediaLane_Audio_Preview() {
    XSaverTheme {
        MediaLane(
            mediaItem = mockLaneItem(MediaType.AUDIO),
            onOpenInX = {}, onShare = {}, onShowDownloadPath = {}, onDelete = {}, onPreview = {}
        )
    }
}
