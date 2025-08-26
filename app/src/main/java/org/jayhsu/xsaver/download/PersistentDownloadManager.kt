package org.jayhsu.xsaver.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okio.buffer
import okio.sink
import org.jayhsu.xsaver.data.dao.DownloadTaskDao
import org.jayhsu.xsaver.data.model.*
import org.jayhsu.xsaver.core.error.DownloadError
import org.jayhsu.xsaver.core.error.toMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File

/**
 * Persistent download manager backed by Room. Simplified version (no partial resume yet).
 */
class PersistentDownloadManager(
    private val baseDir: File,
    private val client: OkHttpClient,
    private val dao: DownloadTaskDao,
    private val externalListener: ((DownloadTask) -> Unit)? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val _inMemory = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    private val _pendingQueue = MutableStateFlow<List<String>>(emptyList())
    private val semaphore = Semaphore(permits = 3) // 限制并发下载数
    private val progressThrottleMs = 400L
    private val cleanupIntervalMs = 60_000L
    private val finishedRetentionMs = 6 * 60 * 60 * 1000L // 6h

    val tasks: Flow<List<DownloadTask>> = dao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .combine(_inMemory) { persisted, mem ->
            // merge in-memory ephemeral states (e.g., immediate progress before DB update flush)
            if (mem.isEmpty()) persisted else persisted.map { mem[it.id] ?: it }
        }

    fun enqueue(task: DownloadTask) {
        scope.launch {
            dao.upsert(task.toEntity())
            scheduleOrQueue(task.id)
        }
    }

    fun pause(id: String) { updateStatus(id, DownloadStatus.Paused) }
    fun cancel(id: String) { updateStatus(id, DownloadStatus.Canceled) }
    fun resume(id: String) { // 简化：重新开始整个下载
        scope.launch {
            val entity = dao.get(id) ?: return@launch
            if (entity.status == DownloadStatus.Completed) return@launch
            dao.updateProgress(id, DownloadStatus.Pending, entity.downloadedBytes, entity.totalBytes, System.currentTimeMillis(), null, null, null)
            scheduleOrQueue(id)
        }
    }

    fun delete(id: String) {
        scope.launch { dao.delete(id) }
    }

    private fun updateStatus(id: String, status: DownloadStatus, error: String? = null, errorType: String? = null, errorCode: Int? = null) {
        scope.launch {
            val entity = dao.get(id) ?: return@launch
            dao.updateProgress(id, status, entity.downloadedBytes, entity.totalBytes, System.currentTimeMillis(), error, errorType, errorCode)
        }
    }

    private fun start(id: String) {
        scope.launch {
            val entity = dao.get(id) ?: return@launch
            if (entity.status == DownloadStatus.Completed) return@launch
            val file = File(baseDir, entity.fileName)
            val existingBytes = if (file.exists()) file.length() else 0L
            val resume = existingBytes > 0L && entity.status == DownloadStatus.Paused
            dao.updateProgress(id, DownloadStatus.Downloading, existingBytes, entity.totalBytes, System.currentTimeMillis(), null, null, null)
            file.parentFile?.mkdirs()
            try {
                semaphore.withPermit {
                val reqBuilder = okhttp3.Request.Builder().url(entity.url)
                if (resume) reqBuilder.addHeader("Range", "bytes=${existingBytes}-")
                val req = reqBuilder.build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val de = DownloadError.Http(resp.code)
                        dao.updateProgress(id, DownloadStatus.Error, 0, entity.totalBytes, System.currentTimeMillis(), de.toMessage(), "Http", resp.code)
                        return@use
                    }
                    val body = resp.body ?: run {
                        val de = DownloadError.EmptyBody
                        dao.updateProgress(id, DownloadStatus.Error, 0, entity.totalBytes, System.currentTimeMillis(), de.toMessage(), "EmptyBody", null); return@use
                    }
                    val contentLength = body.contentLength().takeIf { it >= 0 }
                    val total = if (resume && contentLength != null) existingBytes + contentLength else contentLength?: entity.totalBytes
                    var downloaded = existingBytes
                    val sink = if (resume) file.sink(append = true).buffer() else file.sink().buffer()
                    var lastFlush = System.currentTimeMillis()
                    body.source().use { source ->
                        val bufSize = 8 * 1024
                        val buffer = okio.Buffer()
                        while (true) {
                            val read = source.read(buffer, bufSize.toLong())
                            if (read == -1L) break
                            sink.write(buffer, read)
                            downloaded += read
                            // ephemeral progress update
                            _inMemory.update { map ->
                                val d = (map[id] ?: entity.toDomain()).copy(
                                    status = DownloadStatus.Downloading,
                                    downloadedBytes = downloaded,
                                    totalBytes = total
                                )
                                map + (id to d)
                            }
                            val now = System.currentTimeMillis()
                            if (now - lastFlush > progressThrottleMs) {
                                dao.updateProgress(id, DownloadStatus.Downloading, downloaded, total, now, null, null, null)
                                lastFlush = now
                            }
                            if (!currentCoroutineContext().isActive) break
                        }
                        sink.close()
                    }
                    // persist final state
                    dao.updateProgress(id, DownloadStatus.Completed, downloaded, total, System.currentTimeMillis(), null, null, null)
                    externalListener?.invoke(dao.get(id)!!.toDomain())
                }
                }
            } catch (e: Exception) {
                val de = if (e is java.io.IOException) DownloadError.Io(e.message) else DownloadError.Unknown(e.message)
                val (type, code) = when (de) {
                    is DownloadError.Http -> "Http" to de.code
                    DownloadError.EmptyBody -> "EmptyBody" to null
                    is DownloadError.Io -> "Io" to null
                    is DownloadError.Unknown -> "Unknown" to null
                }
                dao.updateProgress(id, DownloadStatus.Error, 0, entity.totalBytes, System.currentTimeMillis(), de.toMessage(), type, code)
            } finally {
                _inMemory.update { it - id }
                launch { promoteQueue() }
            }
        }
    }

    private fun scheduleOrQueue(id: String) {
        scope.launch { if (semaphore.availablePermits > 0) start(id) else _pendingQueue.update { it + id } }
    }

    private suspend fun promoteQueue() {
        if (semaphore.availablePermits <= 0) return
        val next = _pendingQueue.value.firstOrNull() ?: return
        _pendingQueue.update { it.drop(1) }
        start(next)
    }

    init {
        // 定期清理旧完成任务
        scope.launch {
            while (true) {
                try {
                    val before = System.currentTimeMillis() - finishedRetentionMs
                    dao.cleanupFinished(before)
                } catch (_: Throwable) { }
                delay(cleanupIntervalMs)
            }
        }
    }
}
