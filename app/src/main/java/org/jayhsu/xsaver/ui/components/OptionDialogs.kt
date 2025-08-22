package org.jayhsu.xsaver.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Generic option item for dialogs */
data class OptionItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null
)

enum class SelectionMode { Single, Multiple }

@Composable
private fun <T> OptionSelectableRow(
    item: OptionItem<T>,
    selectionMode: SelectionMode,
    checked: Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = if (selectionMode == SelectionMode.Single) Role.RadioButton else Role.Checkbox) { onSelect(item.value) }
            .padding(vertical = 6.dp)
    ) {
        // Left: optional icon
        if (item.icon != null) {
            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        // Middle: label expands
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = if (item.icon != null) 12.dp else 0.dp, end = 12.dp)
                .weight(1f)
        )
        // Right: selection control
        if (selectionMode == SelectionMode.Single) {
            RadioButton(selected = checked, onClick = { onSelect(item.value) })
        } else {
            Checkbox(checked = checked, onCheckedChange = { onSelect(item.value) })
        }
    }
}

/**
 * Unified selection dialog supporting single (immediate select) and multiple (confirm) modes.
 * Single: radio at far left; Multiple: checkbox at far left. Icon (if provided) appears after control.
 */
@Composable
fun <T> OptionSelectionDialog(
    title: String,
    options: List<OptionItem<T>>,
    selectionMode: SelectionMode,
    singleSelected: T? = null,
    multiSelected: Set<T> = emptySet(),
    onSingleSelect: (T) -> Unit = {},
    onMultiConfirm: (Set<T>) -> Unit = {},
    onDismiss: () -> Unit
) {
    var internalMulti by remember(selectionMode) { mutableStateOf(multiSelected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    val checked = if (selectionMode == SelectionMode.Single) option.value == singleSelected else internalMulti.contains(option.value)
                    OptionSelectableRow(
                        item = option,
                        selectionMode = selectionMode,
                        checked = checked,
                        onSelect = { value ->
                            if (selectionMode == SelectionMode.Single) {
                                onSingleSelect(value)
                                onDismiss()
                            } else {
                                internalMulti = internalMulti.toMutableSet().also { set ->
                                    if (set.contains(value)) set.remove(value) else set.add(value)
                                }
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            if (selectionMode == SelectionMode.Multiple) {
                TextButton(onClick = { onMultiConfirm(internalMulti); onDismiss() }) { Text("OK") }
            }
        },
        dismissButton = {
            if (selectionMode == SelectionMode.Multiple) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// Backwards-compatible overload for previous single-choice usage
@Composable
fun <T> OptionSelectionDialog(
    title: String,
    options: List<OptionItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) = OptionSelectionDialog(
    title = title,
    options = options,
    selectionMode = SelectionMode.Single,
    singleSelected = selected,
    onSingleSelect = onSelect,
    onDismiss = onDismiss
)
