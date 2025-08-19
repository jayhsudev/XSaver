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
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun parseLink(link: String) {
        if (link.isBlank()) {
            _error.value = "链接不能为空"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val mediaItems = repository.parseLink(link.trim())
                _mediaItems.value = mediaItems
            } catch (e: Exception) {
                _error.value = e.message ?: "解析链接失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            try {
                // 从URL提取文件名，如果没有则使用标题或默认名称
                val fileName = extractFileName(mediaItem)
                val success = repository.downloadMedia(mediaItem.url, fileName)
                if (success) {
                    // 下载成功，保存到数据库
                    repository.saveMediaItem(mediaItem)
                } else {
                    _error.value = "下载失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "下载过程中发生错误"
            }
        }
    }

    /**
     * 提取安全的文件名
     */
    private fun extractFileName(mediaItem: MediaItem): String {
        // 首先尝试从URL提取文件名
        val urlFileName = mediaItem.url.substringAfterLast('/')
        if (urlFileName.isNotBlank() && urlFileName.contains('.')) {
            return urlFileName
        }

        // 如果URL没有文件名，使用标题
        val title = mediaItem.title?.takeIf { it.isNotBlank() }
        if (title != null) {
            val extension = when (mediaItem.type) {
                MediaType.VIDEO -> ".mp4"
                MediaType.IMAGE -> ".jpg"
                MediaType.AUDIO -> ".mp3"
            }
            return "$title$extension"
        }

        // 最后使用默认名称
        val extension = when (mediaItem.type) {
            MediaType.VIDEO -> ".mp4"
            MediaType.IMAGE -> ".jpg"
            MediaType.AUDIO -> ".mp3"
        }
        return "media_${System.currentTimeMillis()}$extension"
    }

    fun clearError() {
        _error.value = null
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
        val fileName = extractFileName(mediaItem)
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return "${downloadDir}/${fileName}"
    }
}