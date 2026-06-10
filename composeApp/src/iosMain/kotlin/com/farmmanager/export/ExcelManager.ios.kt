package com.farmmanager.export

import com.farmmanager.data.FarmSnapshot

actual fun platformExport(snapshot: FarmSnapshot): ByteArray = IosExcelBridge.export(snapshot)

actual fun platformImport(bytes: ByteArray): FarmSnapshot = IosExcelBridge.import(bytes)
