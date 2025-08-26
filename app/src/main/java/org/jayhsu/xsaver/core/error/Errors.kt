package org.jayhsu.xsaver.core.error

sealed interface ParseError {
    data object NetworkTimeout : ParseError
    data object StructureChanged : ParseError
    data object Empty : ParseError
    data class Unknown(val reason: String?) : ParseError
}

sealed interface DownloadError {
    data class Http(val code: Int) : DownloadError
    data object EmptyBody : DownloadError
    data class Io(val reason: String?) : DownloadError
    data class Unknown(val reason: String?) : DownloadError
}

fun ParseError.toMessage(): String = when (this) {
    ParseError.NetworkTimeout -> "解析超时"
    ParseError.StructureChanged -> "页面结构已变化，暂无法解析"
    ParseError.Empty -> "未找到媒体内容"
    is ParseError.Unknown -> reason ?: "未知解析错误"
}

fun DownloadError.toMessage(): String = when (this) {
    is DownloadError.Http -> "下载失败 HTTP ${code}"
    DownloadError.EmptyBody -> "下载内容为空"
    is DownloadError.Io -> reason ?: "IO 错误"
    is DownloadError.Unknown -> reason ?: "未知下载错误"
}