package org.jayhsu.xsaver.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _mediaHistory = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaHistory: StateFlow<List<MediaItem>> = _mediaHistory

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

    // 分享媒体
    fun shareMedia(mediaItem: MediaItem): Intent? {
        return repository.shareMedia(mediaItem)
    }

    // 在X上打开
    fun openInX(mediaItem: MediaItem) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(mediaItem.sourceUrl)
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