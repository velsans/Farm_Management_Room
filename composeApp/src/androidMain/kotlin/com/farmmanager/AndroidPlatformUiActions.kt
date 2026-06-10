package com.farmmanager

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.farmmanager.platform.PlatformUiActions
import com.farmmanager.platform.currentActivity

@Composable
fun rememberAndroidPlatformUiActions(): PlatformUiActions {
    val activity = LocalContext.current as ComponentActivity
    var importCallback: ((String?) -> Unit)? = null
    var exportCallback: ((String?) -> Unit)? = null

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importCallback?.invoke(uri?.toString())
        importCallback = null
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ) { uri ->
        exportCallback?.invoke(uri?.toString())
        exportCallback = null
    }

    return remember(activity) {
        object : PlatformUiActions {
            override fun launchImport(onResult: (String?) -> Unit) {
                importCallback = onResult
                importLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            }

            override fun launchExport(defaultFileName: String, onResult: (String?) -> Unit) {
                exportCallback = onResult
                exportLauncher.launch(defaultFileName)
            }

            override fun shareExcel(bytes: ByteArray, filename: String) {
                com.farmmanager.platform.PlatformFileHandler().shareExcel(bytes, filename)
            }

            override fun exitApp() {
                activity.finishAffinity()
            }
        }
    }
}
