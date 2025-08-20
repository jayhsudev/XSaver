package org.jayhsu.xsaver.ui.navigation

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.RowScope
import org.jayhsu.xsaver.R

@Immutable
data class TopBarSpec(
    val title: @Composable () -> Unit,
    val navigationIcon: @Composable () -> Unit = {},
    val actions: @Composable RowScope.() -> Unit = {}
)

interface TopBarController {
    fun set(spec: TopBarSpec?)
    fun setFor(owner: Any, spec: TopBarSpec?)
}

val LocalTopBarController = staticCompositionLocalOf<TopBarController> {
    error("LocalTopBarController not provided")
}

val LocalTopBarSpec = staticCompositionLocalOf<TopBarSpec?> { null }

@Composable
fun TopBarHost(
    content: @Composable () -> Unit,
) {
    var currentTopBarSpec: TopBarSpec? by remember { mutableStateOf(null) }
    var currentOwner: Any? by remember { mutableStateOf(null) }
    val controller = object : TopBarController {
        override fun set(spec: TopBarSpec?) { currentTopBarSpec = spec }
        override fun setFor(owner: Any, spec: TopBarSpec?) {
            if (spec != null) {
                currentOwner = owner
                currentTopBarSpec = spec
            } else {
                if (currentOwner == owner) {
                    currentTopBarSpec = null
                    currentOwner = null
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalTopBarController provides controller,
        LocalTopBarSpec provides currentTopBarSpec
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTopBar(spec: TopBarSpec?) {
    if (spec == null) {
        CenterAlignedTopAppBar(
            title = { Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.titleLarge) }
        )
    } else {
        CenterAlignedTopAppBar(
            title = spec.title,
            navigationIcon = spec.navigationIcon,
            actions = spec.actions
        )
    }
}
