package org.jayhsu.xsaver.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url
import org.jayhsu.xsaver.network.model.LinkParserResponse

interface ApiService {
    // 解析X平台链接的API
    @GET("/api/parse")
    suspend fun parseLink(@Query("url") url: String): LinkParserResponse

    // 下载媒体文件
    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): ResponseBody
}