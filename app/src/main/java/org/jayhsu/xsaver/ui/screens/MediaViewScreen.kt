package org.jayhsu.xsaver.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jayhsu.xsaver.ui.components.MediaViewer
import org.jayhsu.xsaver.ui.components.TweetHeader
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import org.jayhsu.xsaver.ui.theme.XSaverTheme

@Composable
fun MediaViewScreen(
    mediaItem: MediaItem,
    showTweetHeader: Boolean = true
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MediaViewer(media = mediaItem)
            if (showTweetHeader && (mediaItem.accountName != null || mediaItem.url.isNotBlank())) {
                // Scrim + header
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.35f to Color.Black.copy(alpha = 0.10f),
                                1f to Color.Black.copy(alpha = 0.55f)
                            )
                        )
                        .padding(start = 12.dp, end = 12.dp, top = 32.dp, bottom = 12.dp)
                ) {
                    TweetHeader(
                        avatarUrl = mediaItem.avatarUrl,
                        accountName = mediaItem.accountName,
                        text = mediaItem.title ?: mediaItem.url, // fallback to url or title
                        maxCollapsedLines = 3
                    )
                }
            }
        }
    }
}

private fun previewItem(type: MediaType) = MediaItem(
    url = when (type) {
        MediaType.IMAGE -> "https://picsum.photos/900/600"
        MediaType.VIDEO -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        MediaType.AUDIO -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    },
    title = "Preview ${type.name}",
    thumbnailUrl = null,
    type = type,
    sourceUrl = "https://x.com/post/preview",
    avatarUrl = "https://abs.twimg.com/sticky/default_profile_images/default_profile_normal.png",
    accountName = "PreviewUser"
)

@Preview(showBackground = true, name = "MediaViewScreen Image")
@Composable
private fun MediaViewScreen_Image_Preview() { XSaverTheme { MediaViewScreen(previewItem(MediaType.IMAGE)) } }

@Preview(showBackground = true, name = "MediaViewScreen Video")
@Composable
private fun MediaViewScreen_Video_Preview() { XSaverTheme { MediaViewScreen(previewItem(MediaType.VIDEO)) } }

@Preview(showBackground = true, name = "MediaViewScreen Audio")
@Composable
private fun MediaViewScreen_Audio_Preview() { XSaverTheme { MediaViewScreen(previewItem(MediaType.AUDIO)) } }
