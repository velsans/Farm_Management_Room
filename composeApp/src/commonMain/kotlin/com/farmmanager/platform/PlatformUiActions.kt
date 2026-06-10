package com.farmmanager.platform

interface PlatformUiActions {
    fun launchImport(onResult: (String?) -> Unit)
    fun launchExport(defaultFileName: String, onResult: (String?) -> Unit)
    fun shareExcel(bytes: ByteArray, filename: String)
    fun exitApp()
}
