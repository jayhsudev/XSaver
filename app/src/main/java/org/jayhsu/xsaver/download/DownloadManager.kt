package org.jayhsu.xsaver.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import org.jayhsu.xsaver.data.model.DownloadStatus
import org.jayhsu.xsaver.data.model.DownloadTask
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory download manager with pause/resume/delete.
 * Not persisted across process death yet.
 */
class DownloadManager(
    private val baseDir: File,
    private val client: OkHttpClient = OkHttpClient(),
    private val listener: ((DownloadTask) -> Unit)? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks

    private val jobMap = ConcurrentHashMap<String, Job>()

    fun enqueue(task: DownloadTask) {
        update(task)
        startInternal(task.id)
    }

    fun pause(id: String) {
        jobMap[id]?.cancel()
        val t = find(id) ?: return
        update(t.copy(status = DownloadStatus.Paused))
    }

    fun resume(id: String) {
        val t = find(id) ?: return
        if (t.status == DownloadStatus.Paused || t.status == DownloadStatus.Error) {
            update(t.copy(status = DownloadStatus.Pending))
            startInternal(id)
        }
    }

    fun cancel(id: String) {
        jobMap[id]?.cancel()
        val t = find(id) ?: return
        update(t.copy(status = DownloadStatus.Canceled))
    }

    fun delete(id: String) {
        cancel(id)
        val list = _tasks.value.toMutableList()
        val it = list.indexOfFirst { it.id == id }
        if (it >= 0) {
            val task = list.removeAt(it)
            // delete file
            val f = File(baseDir, task.fileName)
            if (f.exists()) runCatching { f.delete() }
            _tasks.value = list
        }
    }

    private fun startInternal(id: String) {
        val task = find(id) ?: return
        val job = scope.launch {
            val file = File(baseDir, task.fileName)
            file.parentFile?.mkdirs()
            update(task.copy(status = DownloadStatus.Downloading))
            try {
                val req = Request.Builder().url(task.url).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        update(task.copy(status = DownloadStatus.Error, error = resp.code.toString()))
                        return@use
                    }
                    val body = resp.body
                    val total = body.contentLength().takeIf { it >= 0 }
                    var downloaded = 0L
                    val sink = file.sink().buffer()
                    body.source().use { source ->
                        val bufSize = 8 * 1024
                        val buffer = okio.Buffer()
                        while (true) {
                            val read = source.read(buffer, bufSize.toLong())
                            if (read == -1L) break
                            sink.write(buffer, read)
                            downloaded += read
                            update(find(id)?.copy(totalBytes = total, downloadedBytes = downloaded, status = DownloadStatus.Downloading) ?: return@use)
                            if (!scope.isActive) break
                        }
                        sink.close()
                    }
                    if (downloaded > 0L && (total == null || downloaded == total)) {
                        val completed = find(id)?.copy(status = DownloadStatus.Completed, downloadedBytes = downloaded) ?: return@use
                        update(completed)
                        listener?.invoke(completed)
                    }
                }
            } catch (e: Exception) {
                update(find(id)?.copy(status = DownloadStatus.Error, error = e.message) ?: return@launch)
                listener?.invoke(find(id) ?: return@launch)
            }
        }
        jobMap[id] = job
    }

    private fun update(task: DownloadTask) {
        val list = _tasks.value.toMutableList()
        val idx = list.indexOfFirst { it.id == task.id }
        if (idx >= 0) list[idx] = task else list += task
        _tasks.value = list
    }

    private fun find(id: String): DownloadTask? = _tasks.value.firstOrNull { it.id == id }
}
