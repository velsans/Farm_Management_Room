package com.farmmanager.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS swipe-back is handled by the navigation stack; no-op for now.
}
