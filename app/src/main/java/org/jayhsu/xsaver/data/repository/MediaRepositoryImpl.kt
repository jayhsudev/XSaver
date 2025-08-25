package org.jayhsu.xsaver.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import org.jayhsu.xsaver.data.dao.MediaDao
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import org.jayhsu.xsaver.network.ApiService
import org.jayhsu.xsaver.network.LinkParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.core.content.FileProvider
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaDao: MediaDao,
    private val apiService: ApiService, // 留着可能的其它下载用途
    private val linkParser: LinkParser,
    @param:ApplicationContext private val context: Context
) : MediaRepository {
    override suspend fun parseLink(link: String): List<MediaItem> {
    val html = linkParser.parseTweetLink(link)
        val parsed = linkParser.parseTweetHtml(html) ?: return emptyList()
        val items = mutableListOf<MediaItem>()
        // Images
        parsed.imageList.forEach { imgUrl ->
            items += MediaItem(
                url = imgUrl,
                title = parsed.text?.take(60),
                thumbnailUrl = imgUrl,
                type = MediaType.IMAGE,
                sourceUrl = link
            )
        }
        // Videos (use first source variant per video)
        parsed.videoList.forEach { video ->
            val first = video.sources.firstOrNull()
            val mediaUrl = first?.url ?: return@forEach
            items += MediaItem(
                url = mediaUrl,
                title = parsed.text?.take(60),
                thumbnailUrl = video.poster,
                type = MediaType.VIDEO,
                sourceUrl = link
            )
        }
        return items
    }

    override fun shareMedia(mediaItem: MediaItem): Intent? {
        // If file downloaded share file else share original url.
        val file = buildLocalFile(mediaItem)
        return if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = if (mediaItem.type == MediaType.IMAGE) "image/*" else if (mediaItem.type == MediaType.VIDEO) "video/*" else "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, mediaItem.url)
            }
        }
    }

    override suspend fun downloadMedia(
        url: String,
        fileName: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dir = downloadDir()
                if (!dir.exists()) dir.mkdirs()
                val target = File(dir, sanitizeFileName(fileName))
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext false
                    val body = resp.body
                    FileOutputStream(target).use { out ->
                        body.byteStream().copyTo(out)
                    }
                }
                true
            } catch (_: Throwable) {
                false
            }
        }
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
    mediaDao.delete(mediaItem)
    }

    override suspend fun deleteAllMediaItems() {
    mediaDao.deleteAll()
    }

    override fun isFileExist(mediaItem: MediaItem): Boolean {
        return buildLocalFile(mediaItem).exists()
    }

    // ---- Helpers ----
    private fun downloadDir(): File = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "XSaver") } ?: File(context.filesDir, "downloads")

    private fun buildLocalFile(mediaItem: MediaItem): File {
        val fileName = sanitizeFileName(extractNameFromUrl(mediaItem.url))
        return File(downloadDir(), fileName)
    }

    private fun extractNameFromUrl(u: String): String = try {
        val path = Uri.parse(u).lastPathSegment ?: "media"
        URLDecoder.decode(path, StandardCharsets.UTF_8.name())
    } catch (_: Throwable) { "media" }

    private fun sanitizeFileName(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]"), "_")

}