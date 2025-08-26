package org.jayhsu.xsaver.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jayhsu.xsaver.ui.components.MediaResultItem
import org.jayhsu.xsaver.ui.theme.XSaverTheme
import org.jayhsu.xsaver.ui.components.TweetHeader
import org.jayhsu.xsaver.data.model.DownloadTask
import org.jayhsu.xsaver.data.model.DownloadStatus
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType

private fun mockMediaItems(): List<MediaItem> {
    return listOf(
        MediaItem(
            id = "1",
            url = "https://example.com/image1.jpg",
            title = "示例图片 1",
            thumbnailUrl = "https://example.com/thumb1.jpg",
            type = MediaType.IMAGE,
            size = 1.2f,
            sourceUrl = "https://x.com/post/1"
        ),
        MediaItem(
            id = "2",
            url = "https://example.com/video1.mp4",
            title = "示例视频 1",
            thumbnailUrl = "https://example.com/thumb2.jpg",
            type = MediaType.VIDEO,
            size = 12.5f,
            duration = 65,
            sourceUrl = "https://x.com/post/2"
        )
    )
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun DownloadScreenPreview_Empty() {
    XSaverTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            androidx.compose.material3.Text(
                text = "点击右下角按钮粘贴X平台链接",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun DownloadScreenPreview_WithList() {
    val items = mockMediaItems()
    XSaverTheme {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items) { item ->
                MediaResultItem(
                    mediaItem = item,
                    onDownloadClick = {},
                    onShareClick = {},
                    onOpenInXClick = {},
                    onShowDownloadPathClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "DownloadScreen Tweet + Tasks")
@Composable
fun DownloadScreenPreview_TweetAndTasks() {
    val items = mockMediaItems()
    val tasks = listOf(
        DownloadTask(url = items[0].url, fileName = "image1.jpg", type = items[0].type, sourceUrl = items[0].sourceUrl, downloadedBytes = 500, totalBytes = 1000, status = DownloadStatus.Downloading),
        DownloadTask(url = items[1].url, fileName = "video1.mp4", type = items[1].type, sourceUrl = items[1].sourceUrl, downloadedBytes = 1000, totalBytes = 1000, status = DownloadStatus.Completed)
    )
    XSaverTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TweetHeader(avatarUrl = "https://abs.twimg.com/sticky/default_profile_images/default_profile_normal.png", accountName = "PreviewUser", text = "这是推文正文的预览")
            androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 12.dp))
            androidx.compose.material3.Text(text = "Active Tasks", style = MaterialTheme.typography.titleSmall)
            tasks.forEach { t ->
                androidx.compose.material3.Text(text = "${t.fileName}  ${(t.progress * 100).toInt()}%  ${t.status}", style = MaterialTheme.typography.bodySmall)
            }
            androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 12.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    MediaResultItem(
                        mediaItem = item,
                        onDownloadClick = {},
                        onShareClick = {},
                        onOpenInXClick = {},
                        onShowDownloadPathClick = {}
                    )
                }
            }
        }
    }
}
