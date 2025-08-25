package org.jayhsu.xsaver.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url
interface ApiService {
    // 下载媒体文件
    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): ResponseBody
}