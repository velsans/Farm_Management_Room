package com.farmmanager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmmanager.data.FarmRepository
import com.farmmanager.export.ExcelManager
import com.farmmanager.platform.PlatformFileHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.farmmanager.util.exportTimestamp

class FarmViewModel(
    private val repository: FarmRepository,
    private val excelManager: ExcelManager,
    private val fileHandler: PlatformFileHandler,
) : ViewModel() {
    val uiState = combine(
        repository.crops,
        repository.expenses,
        repository.harvests,
        repository.sales,
        repository.summaries,
    ) { crops, expenses, harvests, sales, summaries ->
        FarmUiState(crops, expenses, harvests, sales, summaries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FarmUiState())

    var statusMessage by mutableStateOf<String?>(null)
        private set

    fun onIntent(intent: FarmIntent) {
        when (intent) {
            FarmIntent.ClearStatus -> statusMessage = null
            is FarmIntent.AddCrop -> launch("Crop saved") { repository.addCrop(intent.crop) }
            is FarmIntent.AddExpense -> launch("Expense saved") { repository.addExpense(intent.expense) }
            is FarmIntent.AddHarvest -> launch("Harvest saved") { repository.addHarvest(intent.harvest) }
            is FarmIntent.AddSale -> launch("Sale saved") { repository.addSale(intent.sale) }
            is FarmIntent.ExportAll -> exportTo(intent.uri)
            is FarmIntent.ImportAll -> importFrom(intent.uri)
            is FarmIntent.ExportSection -> exportSectionTo(intent.uri, intent.section)
            is FarmIntent.ImportSection -> importSectionFrom(intent.uri, intent.section)
            FarmIntent.ShareExcel -> shareExcel()
        }
    }

    private fun exportTo(uri: String) = viewModelScope.launch {
        runCatching {
            val bytes = excelManager.export(repository.snapshot())
            if (!fileHandler.writeBytes(uri, bytes)) error("Unable to open export file")
        }.onSuccess {
            statusMessage = "Farm data exported to Excel"
        }.onFailure {
            statusMessage = "Export failed: ${it.message}"
        }
    }

    private fun exportSectionTo(uri: String, section: DataSection) = viewModelScope.launch {
        runCatching {
            val bytes = excelManager.export(repository.snapshot().onlySection(section))
            if (!fileHandler.writeBytes(uri, bytes)) error("Unable to open export file")
        }.onSuccess {
            statusMessage = "${section.label} data exported to Excel"
        }.onFailure {
            statusMessage = "Export failed: ${it.message}"
        }
    }

    private fun importFrom(uri: String) = viewModelScope.launch {
        runCatching {
            val bytes = fileHandler.readBytes(uri) ?: error("Unable to open import file")
            repository.mergeImport(excelManager.import(bytes))
        }.onSuccess {
            statusMessage = "Farm data imported without duplicates"
        }.onFailure {
            statusMessage = "Import failed: ${it.message}"
        }
    }

    private fun importSectionFrom(uri: String, section: DataSection) = viewModelScope.launch {
        runCatching {
            val bytes = fileHandler.readBytes(uri) ?: error("Unable to open import file")
            repository.mergeImport(excelManager.import(bytes).onlySection(section))
        }.onSuccess {
            statusMessage = "${section.label} data imported without duplicates"
        }.onFailure {
            statusMessage = "Import failed: ${it.message}"
        }
    }

    private fun shareExcel() = viewModelScope.launch {
        runCatching {
            val bytes = excelManager.export(repository.snapshot())
            fileHandler.shareExcel(bytes, "Farm_Manager_Data_${exportTimestamp()}.xlsx")
        }.onFailure {
            statusMessage = "Share failed: ${it.message}"
        }
    }

    private fun launch(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { statusMessage = success }
                .onFailure { statusMessage = "Save failed: ${it.message}" }
        }
    }
}
