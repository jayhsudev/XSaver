package org.jayhsu.xsaver.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import org.jayhsu.xsaver.ui.components.MediaResultItem
import org.jayhsu.xsaver.ui.components.TweetHeader
import org.jayhsu.xsaver.ui.viewmodel.DownloadViewModel
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.DisposableEffect
import org.jayhsu.xsaver.ui.navigation.LocalTopBarController
import org.jayhsu.xsaver.ui.navigation.TopBarSpec
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import org.jayhsu.xsaver.data.model.DownloadStatus
import org.jayhsu.xsaver.core.error.toMessage
import org.jayhsu.xsaver.ui.designsystem.Dimens
import org.jayhsu.xsaver.ui.screens.download.InlineErrorBanner
import org.jayhsu.xsaver.ui.screens.download.EmptyState
import org.jayhsu.xsaver.ui.screens.download.DownloadTasksPanel
import org.jayhsu.xsaver.ui.viewmodel.DownloadUiEvent
import kotlinx.coroutines.flow.collectLatest

// Inline sealed removed; now using standalone DownloadUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(navController: NavHostController, initialSharedLink: String? = null, onOpenDrawer: () -> Unit = {}, onOpenLatestX: (() -> Unit)? = null) {
    val viewModel: DownloadViewModel = hiltViewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState()
    val uiState by viewModel.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val inlineError = uiState.error
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val mediaItems = uiState.mediaItems
    val isLoading = uiState.parsing
    val parseProgress = uiState.parseProgress
    val parseError = uiState.parseError
    val downloadError = uiState.downloadError
    var showResultSheet by remember { mutableStateOf(false) }
    val selected = remember { mutableStateOf(setOf<String>()) }
    var postText by remember { mutableStateOf("") }
    val parsedTweet = uiState.parsedTweet
    val downloadTasks by viewModel.downloadTasks.collectAsState()

    // Set TopBar via provider
    val topBarController = LocalTopBarController.current
    val topBarOwner = remember { Any() }
    DisposableEffect(topBarOwner) {
        topBarController.setFor(topBarOwner,
            TopBarSpec(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_drawer))
                    }
                },
                actions = {
                    if (onOpenLatestX != null) {
                        IconButton(onClick = onOpenLatestX) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.open_x_app))
                        }
                    }
                }
            )
    )
    onDispose { topBarController.setFor(topBarOwner, null) }
    }

    // If launched with a shared link, parse it once
    if (!initialSharedLink.isNullOrBlank()) {
        // trigger parse and ignore if already loading or has items
        if (!isLoading && mediaItems.isEmpty()) {
            LaunchedEffect(initialSharedLink) {
                viewModel.parseLink(initialSharedLink)
            }
        }
    }

    // 处理解析和下载错误（分别展示）
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { ev ->
            when (ev) {
                is DownloadUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(ev.message)
                is DownloadUiEvent.ShowToast -> Toast.makeText(context, ev.message, Toast.LENGTH_SHORT).show()
                is DownloadUiEvent.ShowSuccess -> {
                    successMessage = ev.message
                    showSuccessDialog = true
                }
                is DownloadUiEvent.Navigate -> navController.navigate(ev.route)
            }
        }
    }

    // When parsing finishes, open results sheet if items

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
    floatingActionButtonPosition = FabPosition.End,
    snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(it)) {

            inlineError?.let { msg -> InlineErrorBanner(message = msg) { /* no-op, driven by state */ } }
            if (mediaItems.isEmpty() && !isLoading) {
                EmptyState(onHistory = { navController.navigate("history") })
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
                        text = stringResource(R.string.parsing_in_progress),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.Space16)
                ) {
                    items(mediaItems, key = { it.id }) { mediaItem ->
                        MediaResultItem(
                            mediaItem = mediaItem,
                            onDownloadClick = {
                                viewModel.downloadMedia(mediaItem)
                                successMessage = context.getString(R.string.start_download, mediaItem.title ?: "")
                                showSuccessDialog = true
                            },
                            onShareClick = {
                                viewModel.shareMedia(mediaItem)?.let { intent ->
                                    context.startActivity(intent)
                                }
                            },
                            onOpenInXClick = { viewModel.openInX(mediaItem) },
                            onShowDownloadPathClick = {
                                val path = viewModel.getDownloadPath(mediaItem)
                                Toast.makeText(context, context.getString(R.string.download_path_toast, path), Toast.LENGTH_LONG).show()
                            },
                            onPreview = {
                                val url = android.net.Uri.encode(mediaItem.url)
                                val type = mediaItem.type.name
                                val title = android.net.Uri.encode(mediaItem.title ?: "")
                                navController.navigate("media?url=${url}&type=${type}&title=${title}") {
                                    launchSingleTop = true
                                    popUpTo(org.jayhsu.xsaver.ui.navigation.Screen.Media.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(Dimens.Space16))
                        Button(onClick = { navController.navigate("history") }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.view_all_history))
                        }
                    }
                }
            }

            // 下载任务进度显示
            DownloadTasksPanel(tasks = downloadTasks, onPause = viewModel::pauseTask, onResume = viewModel::resumeTask, onCancel = viewModel::cancelTask)
        }
    }

    // Removed obsolete parse dialog code

    // 结果全屏BottomSheet（Drawer样式）
    if (showResultSheet) {
        ModalBottomSheet(
            onDismissRequest = { showResultSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Space16)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.identify_results), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showResultSheet = false }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) }
                }
                if (postText.isNotBlank()) {
                    Text(text = postText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = Dimens.Space8))
                }
                parsedTweet?.let { pt ->
                    TweetHeader(avatarUrl = pt.avatarUrl, accountName = pt.accountName, text = pt.text, modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space8))
                }
                Text(text = stringResource(R.string.media_multi_select), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = Dimens.Space8, bottom = Dimens.Space8))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(mediaItems) { _, item ->
                        val checked = selected.value.contains(item.id)
                        val downloaded = viewModel.isDownloaded(item)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (downloaded) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.Green)
                            } else {
                                Checkbox(checked = checked, onCheckedChange = { isChecked ->
                                    selected.value = if (isChecked) selected.value + item.id else selected.value - item.id
                                })
                            }
                            Text(text = (item.title ?: item.url) + if (downloaded) " (downloaded)" else "", modifier = Modifier.padding(start = Dimens.Space8))
                        }
                    }
                }
                Button(
                    onClick = {
                        val toDownload = mediaItems.filter { selected.value.contains(it.id) || viewModel.isDownloaded(it).not() }.ifEmpty { mediaItems.filter { !viewModel.isDownloaded(it) } }
                        toDownload.forEach { viewModel.enqueueDownload(it) }
                        showResultSheet = false
                        successMessage = context.getString(R.string.added_download_tasks, toDownload.size)
                        showSuccessDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.download_selected))
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
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
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
    // 原对话框移除，改为 Snackbar + Inline

    // 成功对话框
    if (showSuccessDialog) {
        AlertDialog(
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.Green) },
        title = { Text(stringResource(R.string.success_title)) },
            text = { Text(successMessage) },
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
            Text(stringResource(R.string.ok))
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