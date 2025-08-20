package org.jayhsu.xsaver.ui.screens

import androidx.compose.runtime.Composable
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.ui.components.MediaViewer
import androidx.compose.runtime.DisposableEffect
import org.jayhsu.xsaver.ui.navigation.LocalTopBarController
import org.jayhsu.xsaver.ui.navigation.TopBarSpec
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import org.jayhsu.xsaver.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.remember

@Composable
fun MediaScreen(media: MediaItem, onBack: () -> Unit = {}) {
    val topBarController = LocalTopBarController.current
    val topBarOwner = remember { Any() }
    DisposableEffect(topBarOwner) {
        topBarController.setFor(topBarOwner,
            TopBarSpec(
                title = { Text(stringResource(R.string.preview)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
    )
    onDispose { topBarController.setFor(topBarOwner, null) }
    }
    MediaViewer(media = media)
}
