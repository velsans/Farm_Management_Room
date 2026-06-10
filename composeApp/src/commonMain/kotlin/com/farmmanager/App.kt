package com.farmmanager

import androidx.compose.runtime.Composable
import com.farmmanager.di.appModule
import com.farmmanager.platform.PlatformUiActions
import com.farmmanager.ui.FarmTheme
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(platformUiActions: PlatformUiActions) {
    KoinApplication(application = {
        modules(appModule)
    }) {
        FarmTheme {
            val viewModel: FarmViewModel = koinViewModel()
            FarmApp(viewModel = viewModel, platformUiActions = platformUiActions)
        }
    }
}
