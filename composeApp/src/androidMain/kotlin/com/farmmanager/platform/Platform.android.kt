package com.farmmanager.platform

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.farmmanager.platform.androidContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun initPlatform() {
}

actual class PlatformFileHandler actual constructor() {
    actual suspend fun readBytes(uri: String): ByteArray? = withContext(Dispatchers.IO) {
        androidContext().contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
    }

    actual suspend fun writeBytes(uri: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        androidContext().contentResolver.openOutputStream(Uri.parse(uri))?.use { stream ->
            stream.write(bytes)
            true
        } ?: false
    }

    actual fun shareExcel(bytes: ByteArray, filename: String) {
        val context = androidContext()
        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(exportDir, filename)
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share farm Excel file")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    actual fun exitApp() {
        currentActivity?.finishAffinity()
    }
}

var currentActivity: androidx.activity.ComponentActivity? = null
