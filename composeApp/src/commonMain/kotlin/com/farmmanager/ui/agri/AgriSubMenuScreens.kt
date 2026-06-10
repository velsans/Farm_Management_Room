package com.farmmanager.ui.agri

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.farmmanager.ui.AppDimens
import farmmanager.composeapp.generated.resources.Res
import farmmanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.farmmanager.DataSection
import com.farmmanager.DetailCard
import com.farmmanager.FarmTab
import com.farmmanager.FarmUiState
import com.farmmanager.HeroCard
import com.farmmanager.ModuleThemeColors
import com.farmmanager.SectionTitle
import com.farmmanager.SectionToolsCard
import com.farmmanager.SectionTotalCard
import com.farmmanager.data.CropEntity
import com.farmmanager.data.ExpenseEntity
import com.farmmanager.data.HarvestEntity
import com.farmmanager.data.SaleEntity
import com.farmmanager.filterByPeriod
import com.farmmanager.formatKg
import com.farmmanager.rupees

@Composable
fun CropsScreen(
    state: FarmUiState,
    selectedMonth: Int?,
    selectedYear: String,
    onMonthSelected: (Int?) -> Unit,
    onYearSelected: (String) -> Unit,
    theme: ModuleThemeColors,
    onImportSection: (DataSection) -> Unit,
    onExportSection: (DataSection) -> Unit,
    onEditCrop: (CropEntity) -> Unit,
) {
    val crops = state.crops.filterByPeriod(selectedMonth, selectedYear) { it.sowingDate }
    val screenPadding = AppDimens.space20
    val itemSpacing = AppDimens.space14
    LazyVerticalGrid(
        columns = GridCells.Adaptive(AppDimens.gridMinCell),
        contentPadding = PaddingValues(start = screenPadding, end = screenPadding, bottom = screenPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionToolsCard(stringResource(Res.string.search_crops), selectedMonth, selectedYear, state.availableYears, theme, onMonthSelected, onYearSelected)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTotalCard(
                title = stringResource(Res.string.crop_total),
                primaryValue = stringResource(Res.string.crop_count, crops.size),
                secondaryValue = stringResource(Res.string.area_acres, crops.sumOf { it.area }),
                icon = Icons.Default.Grass,
                tab = FarmTab.Crops,
            )
        }
        items(crops) { crop ->
            val summary = state.summaries.firstOrNull { it.cropId == crop.id }
            DetailCard(
                title = crop.name,
                subtitle = stringResource(Res.string.crop_details_format, crop.variety, crop.fieldName, crop.area.toString(), crop.season),
                value = rupees(summary?.profitLoss ?: 0.0),
                onClick = { onEditCrop(crop) },
            )
        }
    }
}

@Composable
fun ExpensesScreen(
    state: FarmUiState,
    selectedMonth: Int?,
    selectedYear: String,
    onMonthSelected: (Int?) -> Unit,
    onYearSelected: (String) -> Unit,
    theme: ModuleThemeColors,
    onImportSection: (DataSection) -> Unit,
    onExportSection: (DataSection) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
) {
    val expenses = state.expenses.filterByPeriod(selectedMonth, selectedYear) { it.expenseDate }
    val screenPadding = AppDimens.space20
    LazyColumn(contentPadding = PaddingValues(start = screenPadding, end = screenPadding, bottom = screenPadding), verticalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
        item {
            SectionToolsCard(stringResource(Res.string.search_expenses), selectedMonth, selectedYear, state.availableYears, theme, onMonthSelected, onYearSelected)
        }
        item {
            SectionTotalCard(
                title = stringResource(Res.string.expense_total),
                primaryValue = rupees(expenses.sumOf { it.amount }),
                secondaryValue = stringResource(Res.string.entry_count, expenses.size),
                icon = Icons.Default.WaterDrop,
                tab = FarmTab.Expenses,
            )
        }
        item { SectionTitle(stringResource(Res.string.expenses_title)) }
        items(expenses) { expense ->
            val crop = state.crops.firstOrNull { it.id == expense.cropId }?.name.orEmpty()
            val round = expense.applicationRound?.let { stringResource(Res.string.expense_round_format, it) }.orEmpty()
            DetailCard(
                title = stringResource(Res.string.expense_title_format, expense.category, round),
                subtitle = stringResource(Res.string.expense_subtitle_format, crop, expense.expenseDate),
                value = rupees(expense.amount),
                onClick = { onEditExpense(expense) },
            )
        }
    }
}

@Composable
fun HarvestScreen(
    state: FarmUiState,
    selectedMonth: Int?,
    selectedYear: String,
    onMonthSelected: (Int?) -> Unit,
    onYearSelected: (String) -> Unit,
    theme: ModuleThemeColors,
    onImportSection: (DataSection) -> Unit,
    onExportSection: (DataSection) -> Unit,
    onEditHarvest: (HarvestEntity) -> Unit,
) {
    val harvests = state.harvests.filterByPeriod(selectedMonth, selectedYear) { it.harvestDate }
    val screenPadding = AppDimens.space20
    LazyColumn(contentPadding = PaddingValues(start = screenPadding, end = screenPadding, bottom = screenPadding), verticalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
        item {
            SectionToolsCard(stringResource(Res.string.search_harvest), selectedMonth, selectedYear, state.availableYears, theme, onMonthSelected, onYearSelected)
        }
        item {
            SectionTotalCard(
                title = stringResource(Res.string.harvest_total),
                primaryValue = formatKg(harvests.sumOf { it.quantityKg }),
                secondaryValue = stringResource(Res.string.entry_count, harvests.size),
                icon = Icons.Default.Agriculture,
                tab = FarmTab.Harvest,
            )
        }
        item { SectionTitle(stringResource(Res.string.harvest_management_title)) }
        items(harvests) { harvest ->
            val crop = state.crops.firstOrNull { it.id == harvest.cropId }?.name.orEmpty()
            DetailCard(
                title = stringResource(Res.string.harvest_title_format, crop),
                subtitle = stringResource(Res.string.harvest_subtitle_format, harvest.harvestDate, harvest.managementNotes),
                value = formatKg(harvest.quantityKg),
                onClick = { onEditHarvest(harvest) },
            )
        }
    }
}

@Composable
fun SalesScreen(
    state: FarmUiState,
    selectedMonth: Int?,
    selectedYear: String,
    onMonthSelected: (Int?) -> Unit,
    onYearSelected: (String) -> Unit,
    theme: ModuleThemeColors,
    onImportSection: (DataSection) -> Unit,
    onExportSection: (DataSection) -> Unit,
    onEditSale: (SaleEntity) -> Unit,
) {
    val sales = state.sales.filterByPeriod(selectedMonth, selectedYear) { it.saleDate }
    val screenPadding = AppDimens.space20
    LazyColumn(contentPadding = PaddingValues(start = screenPadding, end = screenPadding, bottom = screenPadding), verticalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
        item {
            SectionToolsCard(stringResource(Res.string.search_sales), selectedMonth, selectedYear, state.availableYears, theme, onMonthSelected, onYearSelected)
        }
        item {
            SectionTotalCard(
                title = stringResource(Res.string.sales_total),
                primaryValue = rupees(sales.sumOf { it.totalIncome }),
                secondaryValue = stringResource(Res.string.entry_count, sales.size),
                icon = Icons.Default.ShoppingCart,
                tab = FarmTab.Sales,
            )
        }
        items(sales) { sale ->
            val crop = state.crops.firstOrNull { it.id == sale.cropId }?.name.orEmpty()
            DetailCard(
                title = stringResource(Res.string.sale_title_format, sale.buyerName, crop),
                subtitle = stringResource(Res.string.sale_subtitle_format, sale.saleDate, formatKg(sale.quantityKg), rupees(sale.pricePerKg), sale.buyerPhone),
                value = rupees(sale.totalIncome),
                onClick = { onEditSale(sale) },
            )
        }
    }
}

@Composable
fun ReportsScreen(state: FarmUiState, onShare: () -> Unit) {
    val screenPadding = AppDimens.space20
    LazyColumn(contentPadding = PaddingValues(screenPadding), verticalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
        item {
            HeroCard(
                title = stringResource(Res.string.offline_report_title),
                value = rupees(state.profitLoss),
                subtitle = stringResource(Res.string.report_subtitle_format, rupees(state.totalIncome), rupees(state.totalExpense)),
                colors = listOf(Color(0xFF00695C), Color(0xFF26A69A), Color(0xFFFFA000)),
            )
        }
        item {
            Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.size(AppDimens.space8))
                Text(stringResource(Res.string.action_share_latest_excel))
            }
        }
    }
}
