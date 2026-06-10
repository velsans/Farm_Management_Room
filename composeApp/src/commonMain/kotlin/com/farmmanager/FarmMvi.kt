package com.farmmanager

import com.farmmanager.data.CropEntity
import com.farmmanager.data.ExpenseEntity
import com.farmmanager.data.HarvestEntity
import com.farmmanager.data.SaleEntity

sealed interface FarmIntent {
    data object ClearStatus : FarmIntent
    data class AddCrop(val crop: CropEntity) : FarmIntent
    data class AddExpense(val expense: ExpenseEntity) : FarmIntent
    data class AddHarvest(val harvest: HarvestEntity) : FarmIntent
    data class AddSale(val sale: SaleEntity) : FarmIntent
    data class ExportAll(val uri: String) : FarmIntent
    data class ImportAll(val uri: String) : FarmIntent
    data class ExportSection(val uri: String, val section: DataSection) : FarmIntent
    data class ImportSection(val uri: String, val section: DataSection) : FarmIntent
    data object ShareExcel : FarmIntent
}

sealed interface FarmEffect {
    data class Message(val text: String) : FarmEffect
}
