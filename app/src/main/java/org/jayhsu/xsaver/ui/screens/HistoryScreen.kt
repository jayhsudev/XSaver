package org.jayhsu.xsaver.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jayhsu.xsaver.R
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import org.jayhsu.xsaver.ui.components.MediaCard
import org.jayhsu.xsaver.ui.components.MediaLane
import org.jayhsu.xsaver.ui.viewmodel.HistoryViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import org.jayhsu.xsaver.ui.navigation.LocalTopBarController
import org.jayhsu.xsaver.ui.navigation.TopBarSpec
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import org.jayhsu.xsaver.ui.viewmodel.SettingsViewModel
import org.jayhsu.xsaver.ui.viewmodel.HistorySortBy
import org.jayhsu.xsaver.ui.viewmodel.HistoryViewMode
import org.jayhsu.xsaver.ui.components.OptionSelectionDialog
import org.jayhsu.xsaver.ui.components.OptionItem
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Storage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onPreview: (MediaItem) -> Unit = {}, onBack: () -> Unit = {}) {
    val viewModel: HistoryViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    val mediaHistory by viewModel.mediaHistory.collectAsState()
    val isMultiSelect by viewModel.isMultiSelect.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val sortByPref by settingsViewModel.historySortBy.collectAsState()
    val viewModePref by settingsViewModel.historyViewMode.collectAsState()
    var selectedMediaType by remember { mutableStateOf<MediaType>(MediaType.VIDEO) } // 默认视频
    var showDeleteDialog by remember { mutableStateOf(false) }
    var mediaToDelete by remember { mutableStateOf<MediaItem?>(null) }
    val topBarController = LocalTopBarController.current
    val topBarOwner = remember { Any() }
    var showMenu by remember { mutableStateOf(false) }
    var showSortDialogLocal by remember { mutableStateOf(false) }
    DisposableEffect(topBarOwner) {
        onDispose { topBarController.setFor(topBarOwner, null) }
    }
    LaunchedEffect(isMultiSelect, selectedIds.size, sortByPref, viewModePref) {
        topBarController.setFor(topBarOwner,
            TopBarSpec(
                title = {
                    if (isMultiSelect) Text(stringResource(R.string.selected_count_format, selectedIds.size))
                    else Text(text = stringResource(id = R.string.history))
                },
                navigationIcon = {
                    if (isMultiSelect) {
                        IconButton(onClick = { viewModel.setMultiSelect(false) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel_multi_select))
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (isMultiSelect) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all))
                        }
                    } else {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more)) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.file_sort)) },
                                onClick = { showMenu = false; showSortDialogLocal = true },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.multi_select)) },
                                onClick = { showMenu = false; viewModel.setMultiSelect(true) },
                                leadingIcon = { Icon(Icons.Filled.ChecklistRtl, contentDescription = null) }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.view_toggle)) },
                                onClick = { showMenu = false; settingsViewModel.toggleHistoryViewMode() },
                                leadingIcon = {
                                    val isList = viewModePref == HistoryViewMode.List
                                    Icon(if (isList) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        )
    }

    val filteredMedia = mediaHistory.filter { it.type == selectedMediaType }

    Column(modifier = Modifier.fillMaxSize()) {
    SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            SegmentedButton(
        selected = selectedMediaType == MediaType.VIDEO,
        onClick = { selectedMediaType = MediaType.VIDEO },
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = null) },
                label = { Text(stringResource(R.string.videos)) }
            )
            SegmentedButton(
        selected = selectedMediaType == MediaType.IMAGE,
        onClick = { selectedMediaType = MediaType.IMAGE },
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Filled.Image, contentDescription = null) },
                label = { Text(stringResource(R.string.images)) }
            )
            SegmentedButton(
        selected = selectedMediaType == MediaType.AUDIO,
        onClick = { selectedMediaType = MediaType.AUDIO },
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Filled.Album, contentDescription = null) },
                label = { Text(stringResource(R.string.audios)) }
            )
        }

    val sortedMedia = when (sortByPref) {
        HistorySortBy.DownloadTime -> filteredMedia.sortedByDescending { it.downloadedAt }
        HistorySortBy.FileSize -> filteredMedia.sortedByDescending { it.size ?: -1f }
    }

    if (sortedMedia.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.empty_history),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            if (viewModePref == HistoryViewMode.List) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(sortedMedia) { mediaItem ->
                        MediaLane(
                            mediaItem = mediaItem,
                            onOpenInX = { viewModel.openInX(mediaItem) },
                            onShare = {
                                viewModel.shareMedia(mediaItem)?.let { intent ->
                                    context.startActivity(intent)
                                }
                            },
                            onShowDownloadPath = {
                                val path = viewModel.getDownloadPath(mediaItem)
                                android.widget.Toast.makeText(context, context.getString(R.string.download_path_toast, path), android.widget.Toast.LENGTH_LONG).show()
                            },
                            onDelete = {
                                mediaToDelete = mediaItem
                                showDeleteDialog = true
                            },
                            onPreview = { onPreview(mediaItem) },
                            showSelection = isMultiSelect,
                            checked = selectedIds.contains(mediaItem.id),
                            onCheckedChange = { _ -> viewModel.toggleSelection(mediaItem.id) }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(sortedMedia) { mediaItem ->
                        MediaCard(
                            mediaItem = mediaItem,
                            onOpenInX = { viewModel.openInX(mediaItem) },
                            onShare = {
                                viewModel.shareMedia(mediaItem)?.let { intent ->
                                    context.startActivity(intent)
                                }
                            },
                            onShowDownloadPath = {
                                val path = viewModel.getDownloadPath(mediaItem)
                                android.widget.Toast.makeText(context, context.getString(R.string.download_path_toast, path), android.widget.Toast.LENGTH_LONG).show()
                            },
                            onDelete = {
                                mediaToDelete = mediaItem
                                showDeleteDialog = true
                            },
                            onPreview = { onPreview(mediaItem) },
                            showSelection = isMultiSelect,
                            checked = selectedIds.contains(mediaItem.id),
                            onCheckedChange = { _ -> viewModel.toggleSelection(mediaItem.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && mediaToDelete != null) {
        AlertDialog(
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    mediaToDelete?.let { viewModel.deleteMedia(it) }
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (isMultiSelect && selectedIds.isNotEmpty()) {
    Surface(tonalElevation = 3.dp) {
            BottomAppBar(modifier = Modifier.navigationBarsPadding()) {
        TextButton(onClick = { viewModel.shareSelected() }) {
                    Text(stringResource(R.string.share))
                }
                TextButton(onClick = { viewModel.deleteSelected() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showSortDialogLocal) {
        OptionSelectionDialog(
            title = stringResource(R.string.file_sort),
            options = listOf(
                OptionItem(HistorySortBy.DownloadTime, stringResource(R.string.sort_by_download_time), Icons.Filled.AccessTime),
                OptionItem(HistorySortBy.FileSize, stringResource(R.string.sort_by_file_size), Icons.Filled.Storage)
            ),
            selected = sortByPref,
            onSelect = { settingsViewModel.setHistorySortBy(it) },
            onDismiss = { showSortDialogLocal = false }
        )
    }
}