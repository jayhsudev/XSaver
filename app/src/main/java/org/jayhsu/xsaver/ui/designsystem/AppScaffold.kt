package org.jayhsu.xsaver.ui.designsystem

import androidx.compose.material3.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Unified Scaffold wrapper that wires snackbarHost, topBar and optional floatingAction.
 * Will be adopted incrementally; currently a thin layer.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    topBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { topBar?.invoke() },
        floatingActionButton = { floatingActionButton?.invoke() },
        content = content
    )
}
