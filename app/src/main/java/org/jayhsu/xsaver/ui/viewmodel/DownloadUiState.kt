package org.jayhsu.xsaver.ui.viewmodel

import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.DownloadTask
import org.jayhsu.xsaver.network.model.ParsedTweet
import org.jayhsu.xsaver.core.error.ParseError
import org.jayhsu.xsaver.core.error.DownloadError

data class DownloadUiState(
    val mediaItems: List<MediaItem> = emptyList(),
    val tasks: List<DownloadTask> = emptyList(),
    val parsedTweet: ParsedTweet? = null,
    val parsing: Boolean = false,
    val parseProgress: Int = 0,
    val error: String? = null,
    val parseError: ParseError? = null,
    val downloadError: DownloadError? = null
)