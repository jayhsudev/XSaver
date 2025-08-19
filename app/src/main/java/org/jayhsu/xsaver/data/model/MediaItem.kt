package org.jayhsu.xsaver.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val type: MediaType,
    val size: Float? = null,
    val duration: Int? = null, // 视频或音频的时长，单位秒
    val sourceUrl: String, // 原始帖子链接
    val downloadedAt: Long = System.currentTimeMillis()
)