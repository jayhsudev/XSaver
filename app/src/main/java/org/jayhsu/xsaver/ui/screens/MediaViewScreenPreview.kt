package org.jayhsu.xsaver.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import org.jayhsu.xsaver.ui.theme.XSaverTheme

private fun previewItem(type: MediaType) = MediaItem(
	url = when (type) {
		MediaType.IMAGE -> "https://picsum.photos/800/500"
		MediaType.VIDEO -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
		MediaType.AUDIO -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
	},
	title = "Preview ${type.name}",
	thumbnailUrl = null,
	type = type,
	sourceUrl = "https://x.com/post/preview"
)

@Preview(showBackground = true, name = "MediaViewScreenPreview Image")
@Composable
fun MediaViewScreenPreview_Image() { XSaverTheme { MediaViewScreen(previewItem(MediaType.IMAGE)) } }

@Preview(showBackground = true, name = "MediaViewScreenPreview Video")
@Composable
fun MediaViewScreenPreview_Video() { XSaverTheme { MediaViewScreen(previewItem(MediaType.VIDEO)) } }

@Preview(showBackground = true, name = "MediaViewScreenPreview Audio")
@Composable
fun MediaViewScreenPreview_Audio() { XSaverTheme { MediaViewScreen(previewItem(MediaType.AUDIO)) } }

