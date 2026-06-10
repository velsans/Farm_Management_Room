package com.farmmanager.platform

expect fun initPlatform()

expect class PlatformFileHandler() {
    suspend fun readBytes(uri: String): ByteArray?
    suspend fun writeBytes(uri: String, bytes: ByteArray): Boolean
    fun shareExcel(bytes: ByteArray, filename: String)
    fun exitApp()
}
