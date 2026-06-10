package com.farmmanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.farmmanager.platform.PlatformUiActions
import com.farmmanager.platform.platformExitApp
import kotlin.system.exitProcess

@Composable
fun rememberIosPlatformUiActions(): PlatformUiActions {
    return remember {
        object : PlatformUiActions {
            override fun launchImport(onResult: (String?) -> Unit) {
                onResult(null)
            }

            override fun launchExport(defaultFileName: String, onResult: (String?) -> Unit) {
                onResult(null)
            }

            override fun shareExcel(bytes: ByteArray, filename: String) {
            }

            override fun exitApp() {
                exitProcess(0)
            }
        }.also {
            platformExitApp = { exitProcess(0) }
        }
    }
}
