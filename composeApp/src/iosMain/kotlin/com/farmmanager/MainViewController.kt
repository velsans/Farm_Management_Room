package com.farmmanager

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    App(platformUiActions = rememberIosPlatformUiActions())
}
