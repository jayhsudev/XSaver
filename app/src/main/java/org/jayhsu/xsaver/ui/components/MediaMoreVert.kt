package org.jayhsu.xsaver.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaMoreVert(
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenInX: () -> Unit,
    onShare: () -> Unit,
    onShowDownloadPath: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextButton(
                onClick = {
                    onOpenInX()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("在X上打开") }

            TextButton(
                onClick = {
                    onShare()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("分享") }

            TextButton(
                onClick = {
                    onShowDownloadPath()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("下载路径") }

            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("删除") }
        }
    }
}
