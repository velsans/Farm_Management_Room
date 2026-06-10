package com.farmmanager.export

import com.farmmanager.data.FarmSnapshot

class ExcelManager {
    fun export(snapshot: FarmSnapshot): ByteArray = platformExport(snapshot)
    fun import(bytes: ByteArray): FarmSnapshot = platformImport(bytes)
}

internal expect fun platformExport(snapshot: FarmSnapshot): ByteArray
internal expect fun platformImport(bytes: ByteArray): FarmSnapshot
