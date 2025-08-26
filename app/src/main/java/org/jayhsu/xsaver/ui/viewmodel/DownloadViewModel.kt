package org.jayhsu.xsaver.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import org.jayhsu.xsaver.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import androidx.core.net.toUri
import org.jayhsu.xsaver.download.DownloadManager
import org.jayhsu.xsaver.download.PersistentDownloadManager
import org.jayhsu.xsaver.data.dao.DownloadTaskDao
import org.jayhsu.xsaver.data.model.DownloadTask
import org.jayhsu.xsaver.data.model.DownloadStatus
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import okhttp3.OkHttpClient
import org.jayhsu.xsaver.network.LinkParser
import org.jayhsu.xsaver.core.error.ParseError
import org.jayhsu.xsaver.core.error.toMessage
import org.jayhsu.xsaver.core.error.DownloadError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val linkParser: LinkParser,
    private val okHttpClient: OkHttpClient,
    private val downloadTaskDao: DownloadTaskDao,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private var lastLink: String? = null
    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState
    private var progressJob: Job? = null
    private val _events = MutableSharedFlow<DownloadUiEvent>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    private val taskMediaMap = mutableMapOf<String, MediaItem>()

    private val downloadManager by lazy {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "XSaver") } ?: File(context.filesDir, "downloads")
    DownloadManager(baseDir = baseDir, client = okHttpClient, listener = { task ->
            if (task.status == DownloadStatus.Completed) {
                taskMediaMap[task.id]?.let { media ->
                    viewModelScope.launch { repository.saveMediaItem(media) }
                }
            }
    })
    }

    private val persistentManager by lazy {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "XSaver") } ?: File(context.filesDir, "downloads")
    PersistentDownloadManager(baseDir = baseDir, client = okHttpClient, dao = downloadTaskDao, externalListener = { completed ->
            taskMediaMap[completed.id]?.let { media ->
                viewModelScope.launch { repository.saveMediaItem(media) }
            }
    })
    }

    private val usePersistent = true

    val downloadTasks: StateFlow<List<DownloadTask>> =
        if (!usePersistent) downloadManager.tasks
    else persistentManager.tasks.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun isDownloaded(mediaItem: MediaItem): Boolean = repository.isFileExist(mediaItem)

    fun parseLink(link: String) {
        if (link.isBlank()) {
            _uiState.value = _uiState.value.copy(error = context.getString(org.jayhsu.xsaver.R.string.link_empty), parseError = ParseError.Unknown("empty"))
            return
        }
    lastLink = link
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(parsing = true, error = null, parseProgress = 0, parsedTweet = null)
            progressJob?.cancel()
            progressJob = launch {
                // Fake determinate progress up to 90% while parsing
                while (_uiState.value.parsing && _uiState.value.parseProgress < 90) {
                    delay(120)
                    _uiState.value = _uiState.value.let { it.copy(parseProgress = (it.parseProgress + 3).coerceAtMost(90)) }
                }
            }
            try {
                val result = linkParser.parseTweetLink(link.trim())
                val items = if (result.tweet != null) repository.parseLink(link.trim()) else emptyList()
                _uiState.value = _uiState.value.copy(
                    mediaItems = items,
                    parsedTweet = result.tweet,
                    parseError = result.error,
                    error = result.error?.toMessage()
                )
                result.error?.toMessage()?.let { _events.tryEmit(DownloadUiEvent.ShowSnackbar(it)) }
            } catch (e: Exception) {
                val classified = ParseError.Unknown(e.message)
                _uiState.value = _uiState.value.copy(parseError = classified, error = classified.toMessage())
            } finally {
                progressJob?.cancel()
                _uiState.value = _uiState.value.copy(parseProgress = 100, parsing = false)
            }
        }
    }

    fun retryParse() { lastLink?.let { parseLink(it) } }

    fun downloadMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            try {
                val fileName = extractFileName(mediaItem)
                val success = repository.downloadMedia(mediaItem.url, fileName)
                if (success) {
                    repository.saveMediaItem(mediaItem)
                } else {
                    val err = DownloadError.Unknown(context.getString(org.jayhsu.xsaver.R.string.download_failed))
                    val msg = err.toMessage()
                    _uiState.value = _uiState.value.copy(error = msg, downloadError = err)
                    _events.tryEmit(DownloadUiEvent.ShowSnackbar(msg))
                }
            } catch (e: Exception) {
                val de = when {
                    e.message?.contains("HTTP", true) == true -> DownloadError.Http(e.message?.filter { it.isDigit() }?.toIntOrNull() ?: -1)
                    e.message?.contains("empty", true) == true -> DownloadError.EmptyBody
                    e is java.io.IOException -> DownloadError.Io(e.message)
                    else -> DownloadError.Unknown(e.message)
                }
                val msg = de.toMessage()
                _uiState.value = _uiState.value.copy(error = msg, downloadError = de)
                _events.tryEmit(DownloadUiEvent.ShowSnackbar(msg))
            }
        }
    }

    private fun extractFileName(mediaItem: MediaItem): String {
        val urlFileName = mediaItem.url.substringAfterLast('/')
        if (urlFileName.isNotBlank() && urlFileName.contains('.')) {
            return urlFileName
        }

        val title = mediaItem.title?.takeIf { it.isNotBlank() }
        if (title != null) {
            val extension = when (mediaItem.type) {
                MediaType.VIDEO -> ".mp4"
                MediaType.IMAGE -> ".jpg"
                MediaType.AUDIO -> ".mp3"
            }
            return "$title$extension"
        }

        val extension = when (mediaItem.type) {
            MediaType.VIDEO -> ".mp4"
            MediaType.IMAGE -> ".jpg"
            MediaType.AUDIO -> ".mp3"
        }
        return "media_${System.currentTimeMillis()}$extension"
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, parseError = null, downloadError = null)
    }

    fun resetParseProgress() { _uiState.value = _uiState.value.copy(parseProgress = 0) }

    fun shareMedia(mediaItem: MediaItem): Intent? {
        return repository.shareMedia(mediaItem)
    }

    fun openInX(mediaItem: MediaItem) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = mediaItem.sourceUrl.toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getDownloadPath(mediaItem: MediaItem): String {
        val fileName = extractFileName(mediaItem)
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return "${downloadDir}/${fileName}"
    }

    fun enqueueDownload(mediaItem: MediaItem) {
        val fileName = extractFileName(mediaItem)
    val existing = downloadTasks.value.firstOrNull { it.url == mediaItem.url && it.fileName == fileName && it.status !in setOf(DownloadStatus.Error, DownloadStatus.Canceled, DownloadStatus.Completed) }
    if (existing != null) return
        val task = DownloadTask(
            url = mediaItem.url,
            fileName = fileName,
            type = mediaItem.type,
            sourceUrl = mediaItem.sourceUrl,
            title = mediaItem.title,
            thumbnailUrl = mediaItem.thumbnailUrl
        )
        taskMediaMap[task.id] = mediaItem
        if (usePersistent) persistentManager.enqueue(task) else downloadManager.enqueue(task)
    }

    fun pauseTask(id: String) { if (usePersistent) persistentManager.pause(id) else downloadManager.pause(id) }
    fun resumeTask(id: String) { if (usePersistent) persistentManager.resume(id) else downloadManager.resume(id) }
    fun cancelTask(id: String) { if (usePersistent) persistentManager.cancel(id) else downloadManager.cancel(id) }
    fun deleteTask(id: String) { if (usePersistent) persistentManager.delete(id) else downloadManager.delete(id) }
}