package org.jayhsu.xsaver.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.jayhsu.xsaver.R
import org.jayhsu.xsaver.ui.screens.DownloadScreen
import org.jayhsu.xsaver.ui.screens.HistoryScreen
import org.jayhsu.xsaver.ui.viewmodel.HistoryViewModel
import org.jayhsu.xsaver.ui.viewmodel.SettingsViewModel
import org.jayhsu.xsaver.ui.viewmodel.ThemeMode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import org.jayhsu.xsaver.ui.viewmodel.HistoryViewModel.SortBy
import java.io.File
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(initialSharedLink: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Reuse HistoryViewModel to get the latest downloaded item
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val mediaHistory by historyViewModel.mediaHistory.collectAsState()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val language by settingsViewModel.language.collectAsState()
    val latestItem = mediaHistory.firstOrNull()
    val isMultiSelect by historyViewModel.isMultiSelect.collectAsState()
    val selectedIds by historyViewModel.selectedIds.collectAsState()
    val sortBy by historyViewModel.sortBy.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(id = R.string.history)) },
                        selected = currentRoute == Screen.History.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.History.route)
                        },
                        icon = { Icon(Icons.Filled.History, contentDescription = null) }
                    )
                    // Show media storage path under the history item
                    val basePath = run {
                        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver")
                        dir.absolutePath
                    }
                    Text(
                        text = "路径: $basePath",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 56.dp, top = 4.dp, bottom = 16.dp)
                    )

                    // Settings placeholders
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text("主题") },
                        selected = false,
                        onClick = { showThemeDialog = true },
                        icon = { /* no icon */ }
                    )
                    NavigationDrawerItem(
                        label = { Text("语言") },
                        selected = false,
                        onClick = { showLanguageDialog = true },
                        icon = { /* no icon */ }
                    )
                    NavigationDrawerItem(
                        label = { Text("清除缓存") },
                        selected = false,
                        onClick = {
                            // Clear internal and external cache
                            try {
                                context.cacheDir?.deleteRecursively()
                                context.externalCacheDir?.deleteRecursively()
                            } catch (_: Exception) {}
                        },
                        icon = { /* no icon */ }
                    )
                }
                // Theme dialog
                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text("主题") },
                        text = {
                            Column {
                                TextButton(onClick = { settingsViewModel.setThemeMode(ThemeMode.LIGHT); showThemeDialog = false }) { Text("亮色模式") }
                                TextButton(onClick = { settingsViewModel.setThemeMode(ThemeMode.DARK); showThemeDialog = false }) { Text("暗色模式") }
                                TextButton(onClick = { settingsViewModel.setThemeMode(ThemeMode.SYSTEM); showThemeDialog = false }) { Text("系统预设") }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {}
                    )
                }
                // Language dialog
                if (showLanguageDialog) {
                    AlertDialog(
                        onDismissRequest = { showLanguageDialog = false },
                        title = { Text("语言") },
                        text = {
                            Column {
                                TextButton(onClick = { settingsViewModel.setLanguage("zh"); showLanguageDialog = false }) { Text("简体中文") }
                                TextButton(onClick = { settingsViewModel.setLanguage("en"); showLanguageDialog = false }) { Text("English") }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {}
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentRoute == Screen.History.route) {
                    CenterAlignedTopAppBar(
                        title = {
                            if (isMultiSelect) Text("已选择 ${selectedIds.size} 项")
                            else Text(text = stringResource(id = R.string.history))
                        },
                        navigationIcon = {
                            if (isMultiSelect) {
                                IconButton(onClick = { historyViewModel.setMultiSelect(false) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "取消多选")
                                }
                            } else {
                                IconButton(onClick = {
                                    scope.launch { drawerState.open() }
                                    // Navigate back to Download
                                    navController.navigate(Screen.Download.route) {
                                        popUpTo(Screen.Download.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            }
                        },
                        actions = {
                            if (isMultiSelect) {
                                IconButton(onClick = { historyViewModel.selectAll() }) {
                                    Icon(Icons.Filled.SelectAll, contentDescription = "全选")
                                }
                            } else {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("文件排序") },
                                        onClick = {
                                            showMenu = false
                                            showSortDialog = true
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("多选") },
                                        onClick = {
                                            showMenu = false
                                            historyViewModel.setMultiSelect(true)
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("视图切换（列表/网格）") },
                                        onClick = {
                                            showMenu = false
                                            historyViewModel.toggleViewMode()
                                        }
                                    )
                                }
                            }
                        }
                    )
                } else if (currentRoute == Screen.Download.route) {
                    CenterAlignedTopAppBar(
                        title = { Text(text = stringResource(id = R.string.app_name)) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "打开侧边栏")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    latestItem?.let { item ->
                                        val uri = item.sourceUrl.toUri()
                                        // Try open X app first, fallback to browser
                                        val candidates = listOf("com.twitter.android", "com.x.android")
                                        var launched = false
                                        for (pkg in candidates) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage(pkg) }
                                                context.startActivity(intent)
                                                launched = true
                                                break
                                            } catch (_: ActivityNotFoundException) {}
                                        }
                                        if (!launched) {
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        }
                                    }
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "打开X应用")
                            }
                        }
                    )
                } else if (currentRoute == Screen.Media.route) {
                    CenterAlignedTopAppBar(
                        title = { Text("预览") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    )
                } else {
                    CenterAlignedTopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Download.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Screen.Download.route) {
                    DownloadScreen(navController, initialSharedLink)
                }
                composable(Screen.History.route) {
                    HistoryScreen(onPreview = { media ->
                        val url = android.net.Uri.encode(media.url)
                        val type = media.type.name
                        val title = android.net.Uri.encode(media.title ?: "")
                        navController.navigate("${Screen.Media.base}?url=${url}&type=${type}&title=${title}")
                    })
                }
                composable(
                    route = Screen.Media.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("url") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val url = backStackEntry.arguments?.getString("url")
                    val typeStr = backStackEntry.arguments?.getString("type")
                    val title = backStackEntry.arguments?.getString("title")
                    if (url != null && typeStr != null) {
                        val media = org.jayhsu.xsaver.data.model.MediaItem(
                            url = url,
                            title = if (title.isNullOrBlank()) null else title,
                            thumbnailUrl = null,
                            type = org.jayhsu.xsaver.data.model.MediaType.valueOf(typeStr),
                            size = null,
                            duration = null,
                            sourceUrl = url
                        )
                        org.jayhsu.xsaver.ui.screens.MediaItemScreen(media)
                    } else {
                        Text("无法加载媒体")
                    }
                }
            }
            // History sort dialog anchored at root
            if (currentRoute == Screen.History.route && showSortDialog) {
                AlertDialog(
                    onDismissRequest = { showSortDialog = false },
                    title = { Text("文件排序") },
                    text = {
                        Column {
                            TextButton(onClick = {
                                historyViewModel.setSortBy(SortBy.DownloadTime)
                                showSortDialog = false
                            }) { Text("按下载时间") }
                            TextButton(onClick = {
                                historyViewModel.setSortBy(SortBy.FileSize)
                                showSortDialog = false
                            }) { Text("按文件大小") }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Download : Screen("download")
    object History : Screen("history")
    object Media : Screen("media?url={url}&type={type}&title={title}") {
        val base = "media"
    }
}