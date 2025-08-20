package org.jayhsu.xsaver.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import androidx.compose.ui.tooling.preview.Preview
import org.jayhsu.xsaver.ui.theme.XSaverTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCard(
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
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onPreview() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
    Box(modifier = Modifier.height(180.dp)) {
            if (mediaItem.type == MediaType.AUDIO) {
                // 音频使用图标背景
        Icon(
            imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AsyncImage(
                    model = mediaItem.thumbnailUrl ?: mediaItem.url,
                    contentDescription = mediaItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (mediaItem.type == MediaType.VIDEO) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 左上角：多选复选框
            if (showSelection) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            // 右上角：分享
            IconButton(onClick = onShare, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Filled.Share, contentDescription = "分享", tint = Color.White)
            }
            // 右下角：更多 -> BottomSheet
            IconButton(onClick = { showSheet = true }, modifier = Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
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

// Previews (kept in the same file per request)
private fun mockItem(type: MediaType) = MediaItem(
    url = when (type) {
        MediaType.IMAGE -> "https://picsum.photos/400/300"
        MediaType.VIDEO -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        MediaType.AUDIO -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    },
    title = when (type) {
        MediaType.IMAGE -> "图片预览"
        MediaType.VIDEO -> "视频预览"
        MediaType.AUDIO -> "音频预览"
    },
    thumbnailUrl = null,
    type = type,
    sourceUrl = "https://x.com/post/mock"
)

@Preview
@Composable
private fun MediaCard_Image_Preview() {
    XSaverTheme {
        MediaCard(
            mediaItem = mockItem(MediaType.IMAGE),
            onOpenInX = {}, onShare = {}, onShowDownloadPath = {}, onDelete = {}, onPreview = {}
        )
    }
}

@Preview
@Composable
private fun MediaCard_Video_Preview() {
    XSaverTheme {
        MediaCard(
            mediaItem = mockItem(MediaType.VIDEO),
            onOpenInX = {}, onShare = {}, onShowDownloadPath = {}, onDelete = {}, onPreview = {}
        )
    }
}

@Preview
@Composable
private fun MediaCard_Audio_Preview() {
    XSaverTheme {
        MediaCard(
            mediaItem = mockItem(MediaType.AUDIO),
            onOpenInX = {}, onShare = {}, onShowDownloadPath = {}, onDelete = {}, onPreview = {}
        )
    }
}
