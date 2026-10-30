package app.cashflow.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform hook that returns the launcher icon for an app id, or null when the
 * platform cannot supply one. Called off the main thread; tiles fall back to a monogram.
 */
val LocalAppIcons = compositionLocalOf<(String) -> ImageBitmap?> { { null } }
