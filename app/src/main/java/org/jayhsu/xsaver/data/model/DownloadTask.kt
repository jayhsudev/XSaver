package org.jayhsu.xsaver.data.model

import org.jayhsu.xsaver.data.model.MediaType
import java.util.UUID

/**
 * Represents a single download task with resumable support.
 */
 data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val fileName: String,
    val type: MediaType,
    val sourceUrl: String,
    // Foreign key referencing TweetEntity.tweetUrl (or its id) for metadata association
    val tweetId: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val totalBytes: Long? = null,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.Pending,
   val error: String? = null,
   val errorType: String? = null,
   val errorCode: Int? = null
 ) {
    val progress: Float
        get() = if (totalBytes != null && totalBytes > 0L) downloadedBytes.toFloat() / totalBytes else 0f
 }

 enum class DownloadStatus { Pending, Downloading, Paused, Completed, Error, Canceled }
