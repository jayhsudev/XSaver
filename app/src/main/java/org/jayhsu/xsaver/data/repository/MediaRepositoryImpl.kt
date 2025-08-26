package org.jayhsu.xsaver.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import org.jayhsu.xsaver.data.dao.MediaDao
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import org.jayhsu.xsaver.network.ApiService
import org.jayhsu.xsaver.data.dao.TweetDao
import org.jayhsu.xsaver.data.model.TweetEntity
import org.jayhsu.xsaver.network.LinkParser
import org.jayhsu.xsaver.core.error.ParseError
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaDao: MediaDao,
    private val tweetDao: TweetDao,
    private val apiService: ApiService, // 留着可能的其它下载用途
    private val linkParser: LinkParser,
    private val okHttpClient: OkHttpClient,
    @param:ApplicationContext private val context: Context
) : MediaRepository {
    // ---- Parse LRU Cache ----
    private data class ParseCacheEntry(val createdAt: Long, val mediaItems: List<MediaItem>)
    private val cacheMutex = Mutex()
    private val parseCacheMax = 32
    private val parseCacheTtlMs = 5 * 60 * 1000L // 5分钟
    private val parseCache = object : LinkedHashMap<String, ParseCacheEntry>(parseCacheMax, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ParseCacheEntry>?): Boolean = size > parseCacheMax
    }

    // ---- File existence cache ----
    private val fileCacheMutex = Mutex()
    @Volatile private var fileNameCache: MutableSet<String>? = null

    private suspend fun ensureFileCache() {
        if (fileNameCache != null) return
        fileCacheMutex.withLock {
            if (fileNameCache == null) {
                val dir = downloadDir()
                val names = if (dir.exists()) dir.list()?.toMutableSet() ?: mutableSetOf() else mutableSetOf()
                fileNameCache = names
            }
        }
    }

    private fun normalizeLink(link: String): String = link.trim().substringBefore('?')

    override suspend fun parseLink(link: String): List<MediaItem> {
        val key = normalizeLink(link)
        // 读缓存
        cacheMutex.withLock {
            parseCache[key]?.let { entry ->
                if (System.currentTimeMillis() - entry.createdAt < parseCacheTtlMs) return entry.mediaItems
                else parseCache.remove(key)
            }
        }
        val result = linkParser.parseTweetLink(key)
        val parsed = result.tweet ?: return emptyList()
        // upsert tweet
        tweetDao.upsert(
            TweetEntity(
                tweetUrl = key,
                avatarUrl = parsed.avatarUrl,
                accountName = parsed.accountName,
                text = parsed.text
            )
        )
        val items = mutableListOf<MediaItem>()
        parsed.imageList.forEach { imgUrl ->
            items += MediaItem(
                url = imgUrl,
                title = parsed.text?.take(60),
                thumbnailUrl = imgUrl,
                type = MediaType.IMAGE,
                sourceUrl = key,
                tweetId = key,
                avatarUrl = parsed.avatarUrl,
                accountName = parsed.accountName
            )
        }
        parsed.videoList.forEach { video ->
            val first = video.sources.firstOrNull()
            val mediaUrl = first?.url ?: return@forEach
            items += MediaItem(
                url = mediaUrl,
                title = parsed.text?.take(60),
                thumbnailUrl = video.poster,
                type = MediaType.VIDEO,
                sourceUrl = key,
                tweetId = key,
                avatarUrl = parsed.avatarUrl,
                accountName = parsed.accountName
            )
        }
        cacheMutex.withLock { parseCache[key] = ParseCacheEntry(System.currentTimeMillis(), items) }
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
                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { resp ->
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
        ensureFileCache()
        val fileName = sanitizeFileName(extractNameFromUrl(mediaItem.url))
        fileNameCache?.add(fileName)
    }

    override suspend fun getTweetCached(url: String) = tweetDao.getTweet(url)

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
        val cache = fileNameCache
        return if (cache != null) {
            val fileName = sanitizeFileName(extractNameFromUrl(mediaItem.url))
            cache.contains(fileName)
        } else buildLocalFile(mediaItem).exists()
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