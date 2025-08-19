package org.jayhsu.xsaver.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.jayhsu.xsaver.R
import org.jayhsu.xsaver.ui.components.MediaPreview
import org.jayhsu.xsaver.ui.viewmodel.DownloadViewModel
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(navController: NavHostController) {
    val viewModel: DownloadViewModel = hiltViewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val mediaItems by viewModel.mediaItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // 处理链接解析结果
    error?.let {
        errorMessage = it
        showErrorDialog = true
        viewModel.clearError()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSheet = true },
                shape = RoundedCornerShape(50),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.paste_link_hint))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(it)) {

            if (mediaItems.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Link,
                        modifier = Modifier.padding(16.dp),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.paste_link_hint),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "点击右下角按钮粘贴X平台链接",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        modifier = Modifier
                            .padding(16.dp)
                            .animateRotate(),
                        contentDescription = null
                    )
                    Text(
                        text = "解析链接中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(mediaItems) { mediaItem ->
                        MediaPreview(
                            mediaItem = mediaItem,
                            onDownloadClick = {
                                viewModel.downloadMedia(mediaItem)
                                successMessage = "开始下载: ${mediaItem.title}"
                                showSuccessDialog = true
                            },
                            onShareClick = {
                                viewModel.shareMedia(mediaItem)?.let { intent ->
                                    context.startActivity(intent)
                                }
                            },
                            onOpenInXClick = {
                                viewModel.openInX(mediaItem)
                            },
                            onShowDownloadPathClick = {
                                val path = viewModel.getDownloadPath(mediaItem)
                                // 显示下载路径，可以使用Toast或Dialog
                                Toast.makeText(context, "下载路径: $path", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // 底部Sheet用于粘贴链接
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.paste_link_hint),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text(stringResource(R.string.paste_link_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                    trailingIcon = {
                        if (link.isNotEmpty()) {
                            IconButton(onClick = { link = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "清除")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            coroutineScope.launch {
                                sheetState.hide()
                                showSheet = false
                                viewModel.parseLink(link)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            showSheet = false
                            viewModel.parseLink(link)
                        }
                    },
                    enabled = link.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.download))
                }
            }
        }
    }

    // 错误对话框
    if (showErrorDialog) {
        AlertDialog(
            icon = { Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("错误") },
            text = { Text(errorMessage) },
            onDismissRequest = { showErrorDialog = false },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    // 成功对话框
    if (showSuccessDialog) {
        AlertDialog(
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.Green) },
            title = { Text("成功") },
            text = { Text(successMessage) },
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

// 旋转动画扩展函数
@Composable
fun Modifier.animateRotate(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    return this.graphicsLayer(rotationZ = rotation)
}