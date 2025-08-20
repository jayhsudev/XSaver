package org.jayhsu.xsaver.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import org.jayhsu.xsaver.ui.theme.XSaverTheme

private fun mockImage(): MediaItem = MediaItem(
    url = "https://picsum.photos/800/600",
    title = "示例图片",
    thumbnailUrl = null,
    type = MediaType.IMAGE,
    sourceUrl = "https://x.com/post/mock"
)

private fun mockVideo(): MediaItem = MediaItem(
    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    title = "示例视频",
    thumbnailUrl = null,
    type = MediaType.VIDEO,
    sourceUrl = "https://x.com/post/mock"
)

private fun mockAudio(): MediaItem = MediaItem(
    url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    title = "示例音频",
    thumbnailUrl = null,
    type = MediaType.AUDIO,
    sourceUrl = "https://x.com/post/mock"
)

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun MediaItemScreen_Image_Preview() {
    XSaverTheme { MediaItemScreen(media = mockImage()) }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun MediaItemScreen_Video_Preview() {
    XSaverTheme { MediaItemScreen(media = mockVideo()) }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun MediaItemScreen_Audio_Preview() {
    XSaverTheme { MediaItemScreen(media = mockAudio()) }
}
