package org.jayhsu.xsaver.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okio.buffer
import okio.sink
import org.jayhsu.xsaver.data.dao.DownloadTaskDao
import org.jayhsu.xsaver.data.model.DownloadStatus
import org.jayhsu.xsaver.core.error.DownloadError
import org.jayhsu.xsaver.core.error.toMessage
import java.io.File

/**
 * WorkManager worker to execute a single download task reliably in background.
 * InputData keys: id, url, fileName
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val dao: DownloadTaskDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE) ?: return Result.failure()
        val dir = File(applicationContext.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "XSaver")
        dir.mkdirs()
        val target = File(dir, fileName)
        return try {
            dao.updateProgress(id, DownloadStatus.Downloading, 0, null, System.currentTimeMillis(), null, null, null)
            val req = okhttp3.Request.Builder().url(url).build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val de = DownloadError.Http(resp.code)
                    dao.updateProgress(id, DownloadStatus.Error, 0, null, System.currentTimeMillis(), de.toMessage(), "Http", resp.code)
                    return Result.retry()
                }
                val body = resp.body ?: run {
                    val de = DownloadError.EmptyBody
                    dao.updateProgress(id, DownloadStatus.Error, 0, null, System.currentTimeMillis(), de.toMessage(), "EmptyBody", null)
                    return Result.failure()
                }
                val total = body.contentLength().takeIf { it >= 0 }
                var downloaded = 0L
                var lastFlush = System.currentTimeMillis()
                val sink = target.sink().buffer()
                body.source().use { source ->
                    val bufSize = 8 * 1024
                    val buffer = okio.Buffer()
                    while (true) {
                        val r = source.read(buffer, bufSize.toLong())
                        if (r == -1L) break
                        sink.write(buffer, r)
                        downloaded += r
                        val now = System.currentTimeMillis()
                        if (now - lastFlush > 400) { // 400ms 节流
                            dao.updateProgress(id, DownloadStatus.Downloading, downloaded, total, now, null, null, null)
                            lastFlush = now
                        }
                        if (isStopped) break
                    }
                    sink.close()
                }
                dao.updateProgress(id, DownloadStatus.Completed, downloaded, total, System.currentTimeMillis(), null, null, null)
            }
            Result.success()
        } catch (e: Exception) {
            val de = when (e) {
                is java.io.IOException -> DownloadError.Io(e.message)
                else -> DownloadError.Unknown(e.message)
            }
            val (type, code) = when (de) {
                is DownloadError.Http -> "Http" to de.code
                DownloadError.EmptyBody -> "EmptyBody" to null
                is DownloadError.Io -> "Io" to null
                is DownloadError.Unknown -> "Unknown" to null
            }
            dao.updateProgress(id, DownloadStatus.Error, 0, null, System.currentTimeMillis(), de.toMessage(), type, code)
            Result.retry()
        }
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_URL = "url"
        const val KEY_FILE = "fileName"
    }
}
