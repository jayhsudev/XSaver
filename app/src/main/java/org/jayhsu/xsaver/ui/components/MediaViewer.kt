package org.jayhsu.xsaver.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import org.jayhsu.xsaver.ui.theme.XSaverTheme
import androidx.core.net.toUri

@Composable
fun MediaViewer(
    media: MediaItem
) {
    when (media.type) {
        MediaType.IMAGE -> ImageViewer(media.url, media.title)
        MediaType.VIDEO, MediaType.AUDIO -> PlayerViewer(media.url)
    }
}

@Composable
private fun ImageViewer(url: String, title: String?) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = url,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@SuppressLint("UseKtx")
@Composable
private fun PlayerViewer(url: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var ready by remember { mutableStateOf(false) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val exoItem = ExoMediaItem.fromUri(url.toUri())
            setMediaItem(exoItem)
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    this.player = player
                }
            }
        )
        if (!ready) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
        }
        LaunchedEffect(player.playbackState) {
            ready = true
        }
    }
}

// Previews inline per request
private fun mockPreviewItem(type: MediaType) = MediaItem(
    url = when (type) {
        MediaType.IMAGE -> "https://picsum.photos/800/600"
        MediaType.VIDEO -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        MediaType.AUDIO -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
    },
    title = "预览 ${type.name}",
    thumbnailUrl = null,
    type = type,
    sourceUrl = "https://x.com/post/mock"
)

@Preview(showBackground = true)
@Composable
private fun MediaViewer_Image_Preview() { XSaverTheme { MediaViewer(media = mockPreviewItem(MediaType.IMAGE)) } }

@Preview(showBackground = true)
@Composable
private fun MediaViewer_Video_Preview() { XSaverTheme { MediaViewer(media = mockPreviewItem(MediaType.VIDEO)) } }

@Preview(showBackground = true)
@Composable
private fun MediaViewer_Audio_Preview() { XSaverTheme { MediaViewer(media = mockPreviewItem(MediaType.AUDIO)) } }