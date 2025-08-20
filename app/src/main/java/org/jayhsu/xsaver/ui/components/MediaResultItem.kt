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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import org.jayhsu.xsaver.ui.theme.XSaverTheme
import org.jayhsu.xsaver.R

@Composable
fun MediaResultItem(
    mediaItem: MediaItem,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenInXClick: () -> Unit,
    onShowDownloadPathClick: () -> Unit,
    onPreview: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onPreview() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            if (mediaItem.type == MediaType.AUDIO) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(64.dp),
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
                        modifier = Modifier.align(Alignment.Center).size(48.dp)
                    )
                }
            }
            IconButton(onClick = onShareClick, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share), tint = Color.White)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = mediaItem.title ?: stringResource(R.string.unnamed),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mediaItem.size?.let { stringResource(R.string.size_in_mb, it) } ?: mediaItem.sourceUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDownloadClick) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.download))
            }
            Row {
                TextButton(onClick = onOpenInXClick) { Text(stringResource(R.string.open_in_x)) }
                TextButton(onClick = onShowDownloadPathClick) { Text(stringResource(R.string.download_path_label)) }
            }
        }
    }
}

// Previews inline per request
private fun mockResultItem(type: MediaType) = MediaItem(
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
private fun MediaResultItem_Image_Preview() {
    XSaverTheme {
        MediaResultItem(
            mediaItem = mockResultItem(MediaType.IMAGE),
            onDownloadClick = {}, onShareClick = {}, onOpenInXClick = {}, onShowDownloadPathClick = {}
        )
    }
}

@Preview
@Composable
private fun MediaResultItem_Video_Preview() {
    XSaverTheme {
        MediaResultItem(
            mediaItem = mockResultItem(MediaType.VIDEO),
            onDownloadClick = {}, onShareClick = {}, onOpenInXClick = {}, onShowDownloadPathClick = {}
        )
    }
}

@Preview
@Composable
private fun MediaResultItem_Audio_Preview() {
    XSaverTheme {
        MediaResultItem(
            mediaItem = mockResultItem(MediaType.AUDIO),
            onDownloadClick = {}, onShareClick = {}, onOpenInXClick = {}, onShowDownloadPathClick = {}
        )
    }
}
