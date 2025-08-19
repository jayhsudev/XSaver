package org.jayhsu.xsaver.network.model

import com.google.gson.annotations.SerializedName

// 链接解析API的响应
data class LinkParserResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<MediaData> = emptyList()
)

// 媒体数据
data class MediaData(
    @SerializedName("url")
    val url: String,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerializedName("type")
    val type: String, // video, image, audio

    @SerializedName("size")
    val size: Float? = null,

    @SerializedName("duration")
    val duration: Int? = null // 视频或音频的时长，单位秒
)