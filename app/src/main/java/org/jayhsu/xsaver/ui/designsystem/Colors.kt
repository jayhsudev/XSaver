package org.jayhsu.xsaver.ui.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme

@Immutable
data class XSaverColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val successContainer: Color,
    val warningContainer: Color,
    val infoContainer: Color,
    val isLight: Boolean
)

// Light defaults (can map to M3 colorScheme, easy to theme later)
fun lightXSaverColors(scheme: ColorScheme) = XSaverColors(
    success = Color(0xFF2E7D32),
    warning = Color(0xFFF57C00),
    info = Color(0xFF0288D1),
    successContainer = Color(0xFFC8E6C9),
    warningContainer = Color(0xFFFFE0B2),
    infoContainer = Color(0xFFB3E5FC),
    isLight = true
)

fun darkXSaverColors(scheme: ColorScheme) = XSaverColors(
    success = Color(0xFF81C784),
    warning = Color(0xFFFFB74D),
    info = Color(0xFF4FC3F7),
    successContainer = Color(0xFF2E7D32),
    warningContainer = Color(0xFFF57C00),
    infoContainer = Color(0xFF0288D1),
    isLight = false
)

val LocalXSaverColors = staticCompositionLocalOf<XSaverColors> {
    error("XSaverColors not provided")
}

// Elevations placeholder (can expand for surfaces)
object Elevations {
    const val Level0 = 0f
    const val Level1 = 1f
    const val Level2 = 3f
    const val Level3 = 6f
    const val Level4 = 8f
    const val Level5 = 12f
}
