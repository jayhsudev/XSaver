package org.jayhsu.xsaver.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_tasks",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val type: MediaType,
    val sourceUrl: String,
    val tweetId: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val totalBytes: Long? = null,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.Pending,
    val error: String? = null,
    val errorType: String? = null,
    val errorCode: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun DownloadTaskEntity.toDomain(): DownloadTask = DownloadTask(
    id = id,
    url = url,
    fileName = fileName,
    type = type,
    sourceUrl = sourceUrl,
    tweetId = tweetId,
    title = title,
    thumbnailUrl = thumbnailUrl,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    status = status,
    error = error,
    errorType = errorType,
    errorCode = errorCode
)

fun DownloadTask.toEntity(now: Long = System.currentTimeMillis()): DownloadTaskEntity = DownloadTaskEntity(
    id = id,
    url = url,
    fileName = fileName,
    type = type,
    sourceUrl = sourceUrl,
    tweetId = tweetId,
    title = title,
    thumbnailUrl = thumbnailUrl,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    status = status,
    error = error,
    errorType = errorType,
    errorCode = errorCode,
    createdAt = now,
    updatedAt = now
)