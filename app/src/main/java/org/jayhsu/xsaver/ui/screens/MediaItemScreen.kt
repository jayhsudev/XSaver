package org.jayhsu.xsaver.ui.screens

import androidx.compose.runtime.Composable
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.ui.components.MediaViewer

@Composable
fun MediaItemScreen(media: MediaItem) {
    MediaViewer(media = media)
}
