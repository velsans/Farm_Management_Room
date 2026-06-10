package com.farmmanager.platform

actual fun initPlatform() {
}

actual class PlatformFileHandler actual constructor() {
    actual suspend fun readBytes(uri: String): ByteArray? = null

    actual suspend fun writeBytes(uri: String, bytes: ByteArray): Boolean = false

    actual fun shareExcel(bytes: ByteArray, filename: String) {
    }

    actual fun exitApp() {
        platformExitApp()
    }
}

lateinit var platformExitApp: () -> Unit
