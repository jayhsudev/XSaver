package org.jayhsu.xsaver.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import org.jayhsu.xsaver.ui.components.MediaResultItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(navController: NavHostController, initialSharedLink: String? = null, onOpenDrawer: () -> Unit = {}, onOpenLatestX: (() -> Unit)? = null) {
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
    val parseProgress by viewModel.parseProgress.collectAsState()
    var showParseDialog by remember { mutableStateOf(false) }
    var showResultSheet by remember { mutableStateOf(false) }
    val selected = remember { mutableStateOf(setOf<String>()) }
    var postText by remember { mutableStateOf("") }

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

    // 处理链接解析结果
    error?.let {
        errorMessage = it
        showErrorDialog = true
        viewModel.clearError()
    }

    // When parsing starts/finishes, control dialog and sheet
    if (isLoading && !showParseDialog) {
        showParseDialog = true
        viewModel.resetParseProgress()
    }
    if (!isLoading && showParseDialog) {
        showParseDialog = false
        // Open results sheet if we have items
        if (mediaItems.isNotEmpty()) {
            showResultSheet = true
        }
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
                        text = stringResource(R.string.paste_link_cta),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("history") }) {
                        Text(stringResource(R.string.view_all_history))
                    }
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
                        text = stringResource(R.string.parsing_in_progress),
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
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navController.navigate("history") }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.view_all_history))
                        }
                    }
                }
            }
        }
    }

    // 解析进度对话框
    if (showParseDialog) {
        AlertDialog(
            onDismissRequest = { /* block while parsing */ },
        title = { Text(stringResource(R.string.parsing_link)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(progress = { parseProgress / 100f }, modifier = Modifier.fillMaxWidth())
            Text(text = stringResource(R.string.percent_format, parseProgress), modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // 结果全屏BottomSheet（Drawer样式）
    if (showResultSheet) {
        ModalBottomSheet(
            onDismissRequest = { showResultSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.identify_results), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showResultSheet = false }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) }
                }
                if (postText.isNotBlank()) {
                    Text(text = postText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                Text(text = stringResource(R.string.media_multi_select), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(mediaItems) { _, item ->
                        val checked = selected.value.contains(item.id)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checked, onCheckedChange = { isChecked ->
                                selected.value = if (isChecked) selected.value + item.id else selected.value - item.id
                            })
                            Text(text = item.title ?: item.url, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                Button(
                    onClick = {
                        val toDownload = mediaItems.filter { selected.value.contains(it.id) }.ifEmpty { mediaItems }
                        viewModel.downloadMediaList(toDownload)
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
    if (showErrorDialog) {
        AlertDialog(
            icon = { Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.error_title)) },
            text = { Text(errorMessage) },
            onDismissRequest = { showErrorDialog = false },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
            Text(stringResource(R.string.ok))
                }
            }
        )
    }

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