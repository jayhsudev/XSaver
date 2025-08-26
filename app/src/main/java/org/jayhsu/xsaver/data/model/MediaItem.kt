package org.jayhsu.xsaver.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "media_items", indices = [Index(value = ["downloadedAt"]), Index(value = ["tweetId"])])
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
    val tweetId: String? = null, // 引用 TweetEntity.tweetUrl
    val avatarUrl: String? = null, // 冗余缓存以便历史列表快速展示
    val accountName: String? = null, // 冗余缓存
    val downloadedAt: Long = System.currentTimeMillis()
)