package org.jayhsu.xsaver.ui.screens

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
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType

private fun mockHistoryItems(): List<MediaItem> {
    return listOf(
        MediaItem(
            id = "h1",
            url = "https://example.com/history1.jpg",
            title = "历史图片 1",
            thumbnailUrl = "https://example.com/hthumb1.jpg",
            type = MediaType.IMAGE,
            size = 2.0f,
            sourceUrl = "https://x.com/post/h1"
        ),
        MediaItem(
            id = "h2",
            url = "https://example.com/history2.mp4",
            title = "历史视频 1",
            thumbnailUrl = "https://example.com/hthumb2.jpg",
            type = MediaType.VIDEO,
            size = 20.0f,
            duration = 120,
            sourceUrl = "https://x.com/post/h2"
        )
    )
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview_Empty() {
    XSaverTheme {
        androidx.compose.material3.Text(
            text = "历史记录为空",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview_WithItems() {
    val items = mockHistoryItems()
    XSaverTheme {
        LazyColumn(modifier = Modifier.fillMaxSize(), content = {
            items(items) { item ->
                MediaResultItem(
                    mediaItem = item,
                    onDownloadClick = {},
                    onShareClick = {},
                    onOpenInXClick = {},
                    onShowDownloadPathClick = {}
                )
            }
        })
    }
}
