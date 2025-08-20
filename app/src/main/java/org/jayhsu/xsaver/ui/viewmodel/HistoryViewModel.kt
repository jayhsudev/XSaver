package org.jayhsu.xsaver.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import androidx.core.content.FileProvider
import androidx.core.net.toUri

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: MediaRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val _mediaHistory = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaHistory: StateFlow<List<MediaItem>> = _mediaHistory

    enum class SortBy { DownloadTime, FileSize }
    enum class ViewMode { List, Grid }

    // UI state
    val isMultiSelect = MutableStateFlow(false)
    val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val sortBy = MutableStateFlow(SortBy.DownloadTime)
    val viewMode = MutableStateFlow(ViewMode.List)

    init {
        // 加载历史记录
        loadMediaHistory()
    }

    private fun loadMediaHistory() {
        viewModelScope.launch {
            repository.getAllMediaItems().collectLatest {
                _mediaHistory.value = it
            }
        }
    }

    fun deleteMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.deleteMediaItem(mediaItem)
        }
    }

    fun deleteAllMedia() {
        viewModelScope.launch {
            repository.deleteAllMediaItems()
        }
    }

    // Multi-select helpers
    fun setMultiSelect(enabled: Boolean) {
        isMultiSelect.value = enabled
        if (!enabled) selectedIds.value = emptySet()
    }

    fun toggleSelection(id: String) {
        val current = selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        selectedIds.value = current
    }

    fun selectAll() {
        selectedIds.value = _mediaHistory.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun setSortBy(newSort: SortBy) {
        sortBy.value = newSort
    }

    fun toggleViewMode() {
        viewMode.value = if (viewMode.value == ViewMode.List) ViewMode.Grid else ViewMode.List
    }

    fun deleteSelected() {
        val ids = selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _mediaHistory.value.filter { it.id in ids }.forEach { repository.deleteMediaItem(it) }
            selectedIds.value = emptySet()
        }
    }

    fun shareSelected() {
        val items = _mediaHistory.value.filter { it.id in selectedIds.value }
        if (items.isEmpty()) return
        // Build multiple share intent
        val uris = ArrayList<Uri>()
        items.forEach { mediaItem ->
            val fileName = mediaItem.url.substringAfterLast('/').ifEmpty { "media_${System.currentTimeMillis()}" }
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver/$fileName")
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                uris.add(uri)
            }
        }
        if (uris.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(Intent.createChooser(intent, context.getString(org.jayhsu.xsaver.R.string.share_media)))
    }

    // 分享媒体
    fun shareMedia(mediaItem: MediaItem): Intent? {
        return repository.shareMedia(mediaItem)
    }

    // 在X上打开
    fun openInX(mediaItem: MediaItem) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = mediaItem.sourceUrl.toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // 显示下载路径
    fun getDownloadPath(mediaItem: MediaItem): String {
        val fileName = mediaItem.url.substringAfterLast('/').ifEmpty {
            "media_${System.currentTimeMillis()}"
        }
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return "${downloadDir}/${fileName}"
    }
}