package org.jayhsu.xsaver.data.repository

import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import kotlinx.coroutines.flow.Flow
import android.content.Intent

interface MediaRepository {
    // 解析链接获取媒体项
    suspend fun parseLink(link: String): List<MediaItem>

    // 分享媒体文件
    fun shareMedia(mediaItem: MediaItem): Intent?

    // 下载媒体文件
    suspend fun downloadMedia(url: String, fileName: String): Boolean

    // 保存媒体项到数据库
    suspend fun saveMediaItem(mediaItem: MediaItem)

    // 获取所有媒体项
    fun getAllMediaItems(): Flow<List<MediaItem>>

    // 按类型获取媒体项
    fun getMediaItemsByType(type: MediaType): Flow<List<MediaItem>>

    // 删除媒体项
    suspend fun deleteMediaItem(mediaItem: MediaItem)

    // 删除所有媒体项
    suspend fun deleteAllMediaItems()

    // 检查文件是否存在
    fun isFileExist(mediaItem: MediaItem): Boolean
}