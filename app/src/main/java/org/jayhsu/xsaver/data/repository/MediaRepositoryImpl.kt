package org.jayhsu.xsaver.data.repository

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.net.Uri
import org.jayhsu.xsaver.data.dao.MediaDao
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import org.jayhsu.xsaver.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaDao: MediaDao,
    private val apiService: ApiService,
    @param:ApplicationContext private val context: Context
) : MediaRepository {

    override suspend fun parseLink(link: String): List<MediaItem> {
        // 验证链接格式
        if (!isValidUrl(link)) {
            throw IllegalArgumentException("无效的链接格式")
        }

        // 调用API服务解析链接
        return try {
            val response = apiService.parseLink(link)
            if (!response.success) {
                throw Exception(response.message ?: "链接解析失败")
            }
            
            // 转换API响应为MediaItem列表
            response.data.map {
                MediaItem(
                    url = it.url,
                    title = it.title,
                    thumbnailUrl = it.thumbnailUrl,
                    type = when (it.type.lowercase()) {
                        "video" -> MediaType.VIDEO
                        "image" -> MediaType.IMAGE
                        "audio" -> MediaType.AUDIO
                        else -> MediaType.IMAGE // 默认为图片
                    },
                    size = it.size,
                    duration = it.duration,
                    sourceUrl = link
                )
            }
        } catch (e: Exception) {
            when (e) {
                is java.net.UnknownHostException -> throw Exception("网络连接失败，请检查网络设置")
                is java.net.SocketTimeoutException -> throw Exception("连接超时，请稍后重试")
                is IllegalArgumentException -> throw e
                else -> throw Exception("解析链接失败: ${e.message}")
            }
        }
    }

    /**
     * 验证URL格式
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme != null && (uri.scheme == "http" || uri.scheme == "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun downloadMedia(url: String, fileName: String): Boolean {
        // 检查存储权限
        if (!checkStoragePermission()) {
            throw Exception("Storage permission is required")
        }

        // 清理文件名，移除不安全字符
        val safeFileName = sanitizeFileName(fileName)

        // 创建下载目录
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        // 创建文件
        val file = File(downloadDir, safeFileName)

        return try {
            // 使用Retrofit下载文件
            val responseBody = apiService.downloadFile(url)
            responseBody.use { body ->
                body.byteStream().use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.flush()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果下载失败，删除部分下载的文件
            if (file.exists()) {
                file.delete()
            }
            false
        }
    }

    /**
     * 清理文件名，移除不安全字符
     */
    private fun sanitizeFileName(fileName: String): String {
        // 移除不安全的字符，替换为下划线
        return fileName
            .replace(Regex("[<>:\"/\\\\|?*]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(100) // 限制文件名长度
            .ifEmpty { "download_${System.currentTimeMillis()}" }
    }

    override suspend fun saveMediaItem(mediaItem: MediaItem) {
        mediaDao.insert(mediaItem)
    }

    override fun getAllMediaItems(): Flow<List<MediaItem>> {
        return mediaDao.getAllMediaItems()
    }

    override fun getMediaItemsByType(type: MediaType): Flow<List<MediaItem>> {
        return mediaDao.getMediaItemsByType(type)
    }

    override suspend fun deleteMediaItem(mediaItem: MediaItem) {
        // 删除数据库记录
        mediaDao.delete(mediaItem)

        // 删除本地文件
        val fileName = extractFileNameFromUrl(mediaItem.url)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver/$fileName")
        if (file.exists()) {
            file.delete()
        }
    }

    override suspend fun deleteAllMediaItems() {
        // 删除数据库所有记录
        mediaDao.deleteAll()

        // 删除所有本地文件
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver")
        if (downloadDir.exists()) {
            downloadDir.deleteRecursively()
        }
    }

    override fun shareMedia(mediaItem: MediaItem): Intent? {
        val fileName = extractFileNameFromUrl(mediaItem.url)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver/$fileName")

        if (!file.exists()) {
            return null
        }

        // 使用 FileProvider 获取文件URI以支持Android 7.0+
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = when (mediaItem.type) {
            MediaType.IMAGE -> "image/*"
            MediaType.VIDEO -> "video/*"
            MediaType.AUDIO -> "audio/*"
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.putExtra(Intent.EXTRA_TITLE, mediaItem.title)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return Intent.createChooser(shareIntent, "分享媒体")
    }

    override fun isFileExist(mediaItem: MediaItem): Boolean {
        val fileName = extractFileNameFromUrl(mediaItem.url)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver/$fileName")
        return file.exists()
    }

    /**
     * 从URL或MediaItem中提取文件名
     */
    private fun extractFileNameFromUrl(url: String): String {
        return url.substringAfterLast('/').ifEmpty { 
            "media_${System.currentTimeMillis()}"
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // 对于Android 13及以上版本，检查媒体权限
            context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // 对于Android 12及以下版本，检查存储权限
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}