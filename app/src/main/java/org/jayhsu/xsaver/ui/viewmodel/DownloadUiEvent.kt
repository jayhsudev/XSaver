package org.jayhsu.xsaver.ui.viewmodel

sealed interface DownloadUiEvent {
    data class ShowSnackbar(val message: String): DownloadUiEvent
    data class ShowToast(val message: String): DownloadUiEvent
    data class ShowSuccess(val message: String): DownloadUiEvent
    data class Navigate(val route: String): DownloadUiEvent
}
