package org.jayhsu.xsaver.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 链接解析API的响应
@Serializable
data class LinkParserResponse(
    @SerialName("success")
    val success: Boolean,

    @SerialName("message")
    val message: String? = null,

    @SerialName("data")
    val data: List<MediaData> = emptyList()
)

// 媒体数据
@Serializable
data class MediaData(
    @SerialName("url")
    val url: String,

    @SerialName("title")
    val title: String? = null,

    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerialName("type")
    val type: String, // video, image, audio

    @SerialName("size")
    val size: Float? = null,

    @SerialName("duration")
    val duration: Int? = null // 视频或音频的时长，单位秒
)