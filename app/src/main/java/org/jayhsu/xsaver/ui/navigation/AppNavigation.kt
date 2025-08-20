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
    val latestItem = remember(mediaHistory) { mediaHistory.maxByOrNull { it.downloadedAt } }
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
                            navController.navigate(Screen.History.route) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.History, contentDescription = null) }
                    )
                    // Show media storage path under the history item
                    val basePath = remember {
                        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "XSaver")
                        dir.absolutePath
                    }
                    Text(
                        text = stringResource(R.string.path_label, basePath),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 56.dp, top = 4.dp, bottom = 16.dp)
                    )

                    // Settings placeholders
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.theme)) },
                        selected = false,
                        onClick = { showThemeDialog = true },
                        icon = { /* no icon */ }
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.language)) },
                        selected = false,
                        onClick = { showLanguageDialog = true },
                        icon = { /* no icon */ }
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.clear_cache)) },
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
            title = { Text(stringResource(R.string.theme)) },
                        text = {
                            Column {
                TextButton(onClick = { settingsViewModel.setThemeMode(ThemeMode.LIGHT); showThemeDialog = false }) { Text(stringResource(R.string.theme_light)) }
                TextButton(onClick = { settingsViewModel.setThemeMode(ThemeMode.DARK); showThemeDialog = false }) { Text(stringResource(R.string.theme_dark)) }
                TextButton(onClick = { settingsViewModel.setThemeMode(ThemeMode.SYSTEM); showThemeDialog = false }) { Text(stringResource(R.string.theme_system)) }
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
            title = { Text(stringResource(R.string.language)) },
                        text = {
                            Column {
                TextButton(onClick = { settingsViewModel.setLanguage("zh"); showLanguageDialog = false }) { Text(stringResource(R.string.language_zh)) }
                TextButton(onClick = { settingsViewModel.setLanguage("en"); showLanguageDialog = false }) { Text(stringResource(R.string.language_en)) }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {}
                    )
                }
            }
        }
    ) {
        TopBarHost {
        Scaffold(
            topBar = { RenderTopBar(LocalTopBarSpec.current) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Download.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Screen.Download.route) {
                    DownloadScreen(
                        navController = navController,
                        initialSharedLink = initialSharedLink,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenLatestX = {
                            latestItem?.let { item ->
                                val uri = item.sourceUrl.toUri()
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
                        }
                    )
                }
                composable(Screen.History.route) {
                    HistoryScreen(
                        onPreview = { media ->
                        val url = android.net.Uri.encode(media.url)
                        val type = media.type.name
                        val title = android.net.Uri.encode(media.title ?: "")
                        navController.navigate("${Screen.Media.base}?url=${url}&type=${type}&title=${title}") {
                            launchSingleTop = true
                        }
                        },
                        onBack = {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                navController.navigate(Screen.Download.route) {
                                    popUpTo(Screen.Download.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
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
                        org.jayhsu.xsaver.ui.screens.MediaScreen(media, onBack = { navController.popBackStack() })
                    } else {
                        Text(stringResource(R.string.cannot_load_media))
                    }
                }
            }
            // History sort dialog anchored at root
            if (currentRoute == Screen.History.route && showSortDialog) {
                AlertDialog(
                    onDismissRequest = { showSortDialog = false },
                    title = { Text(stringResource(R.string.file_sort)) },
                    text = {
                        Column {
                            TextButton(onClick = {
                                historyViewModel.setSortBy(SortBy.DownloadTime)
                                showSortDialog = false
                            }) { Text(stringResource(R.string.sort_by_download_time)) }
                            TextButton(onClick = {
                                historyViewModel.setSortBy(SortBy.FileSize)
                                showSortDialog = false
                            }) { Text(stringResource(R.string.sort_by_file_size)) }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }
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