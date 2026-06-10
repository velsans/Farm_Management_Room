package com.farmmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.farmmanager.data.CropEntity
import com.farmmanager.data.CropSummary
import com.farmmanager.data.ExpenseEntity
import com.farmmanager.data.FarmSnapshot
import com.farmmanager.data.HarvestEntity
import com.farmmanager.data.SaleEntity
import com.farmmanager.platform.PlatformUiActions
import com.farmmanager.ui.AppDimens
import com.farmmanager.ui.FarmTheme
import com.farmmanager.ui.agri.CropsScreen
import com.farmmanager.ui.agri.ExpensesScreen
import com.farmmanager.ui.agri.HarvestScreen
import com.farmmanager.ui.agri.ReportsScreen
import com.farmmanager.ui.agri.SalesScreen
import farmmanager.composeapp.generated.resources.Res
import farmmanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.farmmanager.util.currentMonthNumber
import com.farmmanager.util.currentYearKey
import com.farmmanager.platform.PlatformBackHandler
import com.farmmanager.util.currentMonthNumber
import com.farmmanager.util.currentYearKey
import com.farmmanager.util.exportTimestamp
import com.farmmanager.util.today

private val expenseCategories = listOf("Seed", "Fertilizer", "Labor", "Water Irrigation Labor", "Harvest Management", "Other")
private const val expenseCategorySeed = "Seed"

data class FarmUiState(
    val crops: List<CropEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val harvests: List<HarvestEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val summaries: List<CropSummary> = emptyList(),
) {
    val totalExpense: Double get() = summaries.sumOf { it.totalExpenses }
    val totalIncome: Double get() = summaries.sumOf { it.totalIncome }
    val profitLoss: Double get() = totalIncome - totalExpense
    val harvestedKg: Double get() = summaries.sumOf { it.harvestedKg }
    val availableYears: List<String>
        get() = (
            sales.map { it.saleDate.take(4) } +
                expenses.map { it.expenseDate.take(4) } +
                harvests.map { it.harvestDate.take(4) } +
                crops.map { it.sowingDate.take(4) } +
                currentYearKey()
            )
            .filter { it.length == 4 && it.all(Char::isDigit) }
            .distinct()
            .sortedDescending()

    fun profitLossForPeriod(year: String, month: Int?): Double {
        val periodKey = if (month == null) year else "$year-${month.toString().padStart(2, '0')}"
        val income = sales.filter { it.saleDate.startsWith(periodKey) }.sumOf { it.totalIncome }
        val expense = expenses.filter { it.expenseDate.startsWith(periodKey) }.sumOf { it.amount }
        return income - expense
    }
}

fun FarmSnapshot.onlySection(section: DataSection): FarmSnapshot =
    when (section) {
        DataSection.Crops -> copy(expenses = emptyList(), harvests = emptyList(), sales = emptyList())
        DataSection.Expenses -> copy(harvests = emptyList(), sales = emptyList())
        DataSection.Harvest -> copy(expenses = emptyList(), sales = emptyList())
        DataSection.Sales -> copy(expenses = emptyList(), harvests = emptyList())
    }

enum class FarmTab(val labelRes: StringResource, val icon: ImageVector, val color: Color, val containerColor: Color) {
    Dashboard(Res.string.tab_dashboard, Icons.Default.Home, Color(0xFF1565C0), Color(0xFFBBDEFB)),
    Crops(Res.string.tab_crops, Icons.Default.Spa, Color(0xFF2E7D32), Color(0xFFC8E6C9)),
    Expenses(Res.string.tab_expenses, Icons.Default.Payments, Color(0xFFC62828), Color(0xFFFFCDD2)),
    Harvest(Res.string.tab_harvest, Icons.Default.Agriculture, Color(0xFFEF6C00), Color(0xFFFFE0B2)),
    Sales(Res.string.tab_sales, Icons.Default.ShoppingCart, Color(0xFF6A1B9A), Color(0xFFE1BEE7)),
    Reports(Res.string.tab_reports, Icons.Default.Assessment, Color(0xFF00695C), Color(0xFFB2DFDB)),
}

enum class DataSection(val label: String, val filePrefix: String, val tab: FarmTab) {
    Crops("Crops", "Farm_Crops", FarmTab.Crops),
    Expenses("Expenses", "Farm_Expenses", FarmTab.Expenses),
    Harvest("Harvest", "Farm_Harvest", FarmTab.Harvest),
    Sales("Sales", "Farm_Sales", FarmTab.Sales),
}

enum class FarmModuleTab(val labelRes: StringResource, val titleRes: StringResource, val subtitleRes: StringResource, val icon: ImageVector) {
    Agri(Res.string.module_agri, Res.string.module_agri_title, Res.string.module_agri_subtitle, Icons.Default.Agriculture),
    Goat(Res.string.module_goat, Res.string.module_goat_title, Res.string.module_goat_subtitle, Icons.Default.Pets),
    Chicken(Res.string.module_chicken, Res.string.module_chicken_title, Res.string.module_chicken_subtitle, Icons.Default.EggAlt),
}

data class MonthOption(val number: Int?, val label: String)

val MonthOptions = listOf(
    MonthOption(null, "All Months"),
    MonthOption(1, "January"),
    MonthOption(2, "February"),
    MonthOption(3, "March"),
    MonthOption(4, "April"),
    MonthOption(5, "May"),
    MonthOption(6, "June"),
    MonthOption(7, "July"),
    MonthOption(8, "August"),
    MonthOption(9, "September"),
    MonthOption(10, "October"),
    MonthOption(11, "November"),
    MonthOption(12, "December"),
)

data class ModuleThemeColors(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surface: Color,
    val gradient: List<Color>,
)

fun FarmModuleTab.themeColors(): ModuleThemeColors =
    when (this) {
        FarmModuleTab.Agri -> ModuleThemeColors(
            primary = Color(0xFF2E7D32),
            primaryContainer = Color(0xFFC8E6C9),
            onPrimaryContainer = Color(0xFF05350A),
            surface = Color(0xFFF1F8E9),
            gradient = listOf(Color(0xFF1B5E20), Color(0xFF43A047), Color(0xFFFFB300)),
        )
        FarmModuleTab.Goat -> ModuleThemeColors(
            primary = Color(0xFF6D4C41),
            primaryContainer = Color(0xFFD7CCC8),
            onPrimaryContainer = Color(0xFF2A120B),
            surface = Color(0xFFFFF3E0),
            gradient = listOf(Color(0xFF4E342E), Color(0xFF8D6E63), Color(0xFFFFB300)),
        )
        FarmModuleTab.Chicken -> ModuleThemeColors(
            primary = Color(0xFFD84315),
            primaryContainer = Color(0xFFFFCCBC),
            onPrimaryContainer = Color(0xFF3E1000),
            surface = Color(0xFFFFF8E1),
            gradient = listOf(Color(0xFFBF360C), Color(0xFFFF7043), Color(0xFFFFCA28)),
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmApp(
    viewModel: FarmViewModel,
    platformUiActions: PlatformUiActions,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<FarmTab?>(null) }
    var editingCrop by remember { mutableStateOf<CropEntity?>(null) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var editingHarvest by remember { mutableStateOf<HarvestEntity?>(null) }
    var editingSale by remember { mutableStateOf<SaleEntity?>(null) }
    var selectedModule by remember { mutableStateOf(FarmModuleTab.Agri) }
    var selectedAgriTab by remember { mutableStateOf(FarmTab.Dashboard) }
    var showExitDialog by remember { mutableStateOf(false) }
    val moduleTheme = selectedModule.themeColors()
    var pendingExportSection by remember { mutableStateOf<DataSection?>(null) }
    var pendingImportSection by remember { mutableStateOf<DataSection?>(null) }

    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onIntent(FarmIntent.ClearStatus)
        }
    }

    val atAgriDashboard = selectedModule == FarmModuleTab.Agri && selectedAgriTab == FarmTab.Dashboard
    PlatformBackHandler(enabled = true) {
        when {
            showExitDialog -> showExitDialog = false
            dialog != null -> dialog = null
            editingCrop != null -> editingCrop = null
            editingExpense != null -> editingExpense = null
            editingHarvest != null -> editingHarvest = null
            editingSale != null -> editingSale = null
            !atAgriDashboard -> {
                selectedModule = FarmModuleTab.Agri
                selectedAgriTab = FarmTab.Dashboard
            }
            else -> showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(Res.string.exit_app_title)) },
            text = { Text(stringResource(Res.string.exit_app_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        platformUiActions.exitApp()
                    },
                ) { Text(stringResource(Res.string.exit_app_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(Res.string.exit_app_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = moduleTheme.primaryContainer,
                    titleContentColor = moduleTheme.onPrimaryContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedModule == FarmModuleTab.Agri && selectedAgriTab == FarmTab.Dashboard) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
                    BottomDataActionButton(
                        label = stringResource(Res.string.action_import),
                        icon = Icons.Default.UploadFile,
                        buttonColor = Color(0xFF1B5E20),
                        iconBackgroundColor = Color(0xFF0B3D0E),
                        iconTint = Color.White,
                        contentColor = Color.White,
                        onClick = {
                            pendingImportSection = null
                            platformUiActions.launchImport { uri ->
                                uri?.let { viewModel.onIntent(FarmIntent.ImportAll(it)) }
                            }
                        },
                    )
                    BottomDataActionButton(
                        label = stringResource(Res.string.action_export),
                        icon = Icons.Default.FileDownload,
                        buttonColor = Color(0xFF263238),
                        iconBackgroundColor = Color(0xFF11171A),
                        iconTint = Color.White,
                        contentColor = Color.White,
                        onClick = {
                            pendingExportSection = null
                            platformUiActions.launchExport("Farm_Manager_Data.xlsx") { uri ->
                                uri?.let { viewModel.onIntent(FarmIntent.ExportAll(it)) }
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            DashboardScreen(
                state = state,
                selectedModule = selectedModule,
                onModuleSelected = {
                    selectedModule = it
                    selectedAgriTab = FarmTab.Dashboard
                },
                selectedAgriTab = selectedAgriTab,
                onAgriTabSelected = { selectedAgriTab = it },
                onShare = { viewModel.onIntent(FarmIntent.ShareExcel) },
                onAdd = { dialog = it },
                onImportSection = { section ->
                    pendingImportSection = section
                    platformUiActions.launchImport { uri ->
                        uri?.let { viewModel.onIntent(FarmIntent.ImportSection(it, section)) }
                    }
                },
                onExportSection = { section ->
                    pendingExportSection = section
                    platformUiActions.launchExport("${section.filePrefix}.xlsx") { uri ->
                        uri?.let { viewModel.onIntent(FarmIntent.ExportSection(it, section)) }
                    }
                },
                onEditCrop = { editingCrop = it },
                onEditExpense = { editingExpense = it },
                onEditHarvest = { editingHarvest = it },
                onEditSale = { editingSale = it },
            )
        }
    }

    when (dialog) {
        FarmTab.Crops -> CropDialog(onDismiss = { dialog = null }, onSave = { viewModel.onIntent(FarmIntent.AddCrop(it)); dialog = null })
        FarmTab.Expenses -> ExpenseDialog(state.crops, onDismiss = { dialog = null }, onSave = { viewModel.onIntent(FarmIntent.AddExpense(it)); dialog = null })
        FarmTab.Harvest -> HarvestDialog(state.crops, onDismiss = { dialog = null }, onSave = { viewModel.onIntent(FarmIntent.AddHarvest(it)); dialog = null })
        FarmTab.Sales -> SaleDialog(state.crops, onDismiss = { dialog = null }, onSave = { viewModel.onIntent(FarmIntent.AddSale(it)); dialog = null })
        else -> Unit
    }
    editingCrop?.let { crop ->
        CropDialog(
            initial = crop,
            onDismiss = { editingCrop = null },
            onSave = {
                viewModel.onIntent(FarmIntent.AddCrop(it))
                editingCrop = null
            },
        )
    }
    editingExpense?.let { expense ->
        ExpenseDialog(
            crops = state.crops,
            initial = expense,
            onDismiss = { editingExpense = null },
            onSave = {
                viewModel.onIntent(FarmIntent.AddExpense(it))
                editingExpense = null
            },
        )
    }
    editingHarvest?.let { harvest ->
        HarvestDialog(
            crops = state.crops,
            initial = harvest,
            onDismiss = { editingHarvest = null },
            onSave = {
                viewModel.onIntent(FarmIntent.AddHarvest(it))
                editingHarvest = null
            },
        )
    }
    editingSale?.let { sale ->
        SaleDialog(
            crops = state.crops,
            initial = sale,
            onDismiss = { editingSale = null },
            onSave = {
                viewModel.onIntent(FarmIntent.AddSale(it))
                editingSale = null
            },
        )
    }
}

@Composable
fun DashboardScreen(
    state: FarmUiState,
    selectedModule: FarmModuleTab,
    onModuleSelected: (FarmModuleTab) -> Unit,
    selectedAgriTab: FarmTab,
    onAgriTabSelected: (FarmTab) -> Unit,
    onShare: () -> Unit,
    onAdd: (FarmTab) -> Unit,
    onImportSection: (DataSection) -> Unit,
    onExportSection: (DataSection) -> Unit,
    onEditCrop: (CropEntity) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onEditHarvest: (HarvestEntity) -> Unit,
    onEditSale: (SaleEntity) -> Unit,
) {
    val moduleTheme = selectedModule.themeColors()

    Column(Modifier.fillMaxSize()) {
        FarmModuleTabs(
            selectedModule = selectedModule,
            onSelected = onModuleSelected,
            theme = moduleTheme,
            modifier = Modifier.padding(AppDimens.space20),
        )
        when (selectedModule) {
            FarmModuleTab.Agri -> AgriDashboardTabs(
                state = state,
                onShare = onShare,
                onAdd = onAdd,
                onImportSection = onImportSection,
                onExportSection = onExportSection,
                onEditCrop = onEditCrop,
                onEditExpense = onEditExpense,
                onEditHarvest = onEditHarvest,
                onEditSale = onEditSale,
                selectedAgriTab = selectedAgriTab,
                onAgriTabSelected = onAgriTabSelected,
                theme = moduleTheme,
            )
            FarmModuleTab.Goat -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = AppDimens.space20, end = AppDimens.space20, bottom = AppDimens.space20),
                verticalArrangement = Arrangement.spacedBy(AppDimens.space16),
            ) {
                futureModuleDashboardItems(
                    module = FarmModuleTab.Goat,
                    theme = moduleTheme,
                )
            }
            FarmModuleTab.Chicken -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = AppDimens.space20, end = AppDimens.space20, bottom = AppDimens.space20),
                verticalArrangement = Arrangement.spacedBy(AppDimens.space16),
            ) {
                futureModuleDashboardItems(
                    module = FarmModuleTab.Chicken,
                    theme = moduleTheme,
                )
            }
        }
    }
}

@Composable
fun AgriDashboardTabs(
    state: FarmUiState,
    onShare: () -> Unit,
    onAdd: (FarmTab) -> Unit,
    onImportSection: (DataSection) -> Unit,
    onExportSection: (DataSection) -> Unit,
    onEditCrop: (CropEntity) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onEditHarvest: (HarvestEntity) -> Unit,
    onEditSale: (SaleEntity) -> Unit,
    selectedAgriTab: FarmTab,
    onAgriTabSelected: (FarmTab) -> Unit,
    theme: ModuleThemeColors,
) {
    var selectedMonth by remember { mutableStateOf<Int?>(currentMonthNumber()) }
    var selectedYear by remember { mutableStateOf(currentYearKey()) }

    Column(Modifier.fillMaxSize()) {
        AgriNavigationTabs(
            selectedTab = selectedAgriTab,
            onSelected = onAgriTabSelected,
            theme = theme,
            modifier = Modifier.padding(horizontal = AppDimens.space30),
        )
        Spacer(Modifier.height(AppDimens.space5))

        Box(Modifier.weight(1f)) {
            when (selectedAgriTab) {
                FarmTab.Dashboard -> LazyColumn(
                    contentPadding = PaddingValues(start = AppDimens.space20, end = AppDimens.space20, bottom = AppDimens.space20),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.space16),
                ) {
                    agricultureDashboardItems(
                        state = state,
                        onShare = onShare,
                        theme = theme,
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        onMonthSelected = { selectedMonth = it },
                        onYearSelected = { selectedYear = it },
                        onNavigate = onAgriTabSelected,
                    )
                }
                FarmTab.Crops -> CropsScreen(state, selectedMonth, selectedYear, { selectedMonth = it }, { selectedYear = it }, theme, onImportSection, onExportSection, onEditCrop)
                FarmTab.Expenses -> ExpensesScreen(state, selectedMonth, selectedYear, { selectedMonth = it }, { selectedYear = it }, theme, onImportSection, onExportSection, onEditExpense)
                FarmTab.Harvest -> HarvestScreen(state, selectedMonth, selectedYear, { selectedMonth = it }, { selectedYear = it }, theme, onImportSection, onExportSection, onEditHarvest)
                FarmTab.Sales -> SalesScreen(state, selectedMonth, selectedYear, { selectedMonth = it }, { selectedYear = it }, theme, onImportSection, onExportSection, onEditSale)
                FarmTab.Reports -> ReportsScreen(state, onShare)
            }
            if (selectedAgriTab != FarmTab.Dashboard && selectedAgriTab != FarmTab.Reports) {
                FloatingActionButton(
                    onClick = { onAdd(selectedAgriTab) },
                    containerColor = theme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = AppDimens.space20, bottom = AppDimens.space92),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.action_add_content_description, stringResource(selectedAgriTab.labelRes)))
                }
            }
        }
    }
}

fun LazyListScope.agricultureDashboardItems(
    state: FarmUiState,
    onShare: () -> Unit,
    theme: ModuleThemeColors,
    selectedMonth: Int?,
    selectedYear: String,
    onMonthSelected: (Int?) -> Unit,
    onYearSelected: (String) -> Unit,
    onNavigate: (FarmTab) -> Unit,
) {
    val selectedPeriodProfitLoss = state.profitLossForPeriod(selectedYear, selectedMonth)
    item {
        ProfitPeriodFilterCard(
            title = stringResource(Res.string.search_profit_loss),
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            years = state.availableYears,
            theme = theme,
            onMonthSelected = onMonthSelected,
            onYearSelected = onYearSelected,
        )
    }
    item {
        val periodLabel = periodLabel(selectedMonth, selectedYear)
        HeroCard(
            title = stringResource(Res.string.profit_loss_title_format, periodLabel),
            value = rupees(selectedPeriodProfitLoss),
            subtitle = stringResource(Res.string.profit_loss_subtitle_format, rupees(state.profitLoss), state.crops.size, formatKg(state.harvestedKg)),
            colors = theme.gradient,
        )
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space12), modifier = Modifier.fillMaxWidth()) {
            MetricCard(stringResource(Res.string.expenses_metric), rupees(state.totalExpense), Icons.Default.WaterDrop, Modifier.weight(1f), onClick = { onNavigate(FarmTab.Expenses) })
            MetricCard(stringResource(Res.string.sales_metric), rupees(state.totalIncome), Icons.Default.ShoppingCart, Modifier.weight(1f), onClick = { onNavigate(FarmTab.Sales) })
        }
    }
    item {
        OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Share, null)
            Spacer(Modifier.size(AppDimens.space8))
            Text(stringResource(Res.string.action_share_excel_external))
        }
    }
}
@Composable
fun ProfitPeriodFilterCard(
    title: String,
    selectedMonth: Int?,
    selectedYear: String,
    years: List<String>,
    theme: ModuleThemeColors,
    onMonthSelected: (Int?) -> Unit,
    onYearSelected: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.surface),
        shape = RoundedCornerShape(AppDimens.radiusXLarge),
    ) {
        Column(Modifier.padding(AppDimens.space14), verticalArrangement = Arrangement.spacedBy(AppDimens.space14)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = theme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space12), modifier = Modifier.fillMaxWidth()) {
                MonthDropdown(
                    selectedMonth = selectedMonth,
                    onSelected = onMonthSelected,
                    theme = theme,
                    modifier = Modifier.weight(1f),
                )
                YearDropdown(
                    selectedYear = selectedYear,
                    years = years,
                    onSelected = onYearSelected,
                    theme = theme,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun MonthDropdown(selectedMonth: Int?, onSelected: (Int?) -> Unit, theme: ModuleThemeColors, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = MonthOptions.firstOrNull { it.number == selectedMonth }?.label ?: stringResource(Res.string.month_placeholder)

    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedLabel, color = theme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MonthOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.number)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun YearDropdown(selectedYear: String, years: List<String>, onSelected: (String) -> Unit, theme: ModuleThemeColors, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedYear, color = theme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year) },
                    onClick = {
                        onSelected(year)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SectionToolsCard(
    title: String,
    selectedMonth: Int?,
    selectedYear: String,
    years: List<String>,
    theme: ModuleThemeColors,
    onMonthSelected: (Int?) -> Unit,
    onYearSelected: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.surface),
        shape = RoundedCornerShape(AppDimens.radiusXLarge),
    ) {
        Column(Modifier.padding(AppDimens.space12), verticalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = theme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space12), modifier = Modifier.fillMaxWidth()) {
                MonthDropdown(selectedMonth, onMonthSelected, theme, Modifier.weight(1f))
                YearDropdown(selectedYear, years, onYearSelected, theme, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BottomDataActionButton(
    label: String,
    icon: ImageVector,
    buttonColor: Color,
    iconBackgroundColor: Color,
    iconTint: Color,
    contentColor: Color,
    onClick: () -> Unit
) {

    FloatingActionButton(
        onClick = onClick,
        containerColor = buttonColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(AppDimens.radiusMedium),
    ) {

        Row(
            modifier = Modifier.padding(horizontal = AppDimens.space14),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space6),
        ) {

            Box(
                modifier = Modifier
                    .size(AppDimens.iconContainer)
                    .background(
                        color = iconBackgroundColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(AppDimens.iconSmall),
                    tint = iconTint
                )
            }
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}
fun LazyListScope.futureModuleDashboardItems(module: FarmModuleTab, theme: ModuleThemeColors) {
    item {
        HeroCard(
            title = stringResource(module.titleRes),
            value = stringResource(Res.string.coming_soon),
            subtitle = stringResource(module.subtitleRes),
            colors = theme.gradient,
        )
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space12), modifier = Modifier.fillMaxWidth()) {
            MetricCard(stringResource(Res.string.expenses_metric), stringResource(Res.string.zero_rupees), Icons.Default.WaterDrop, Modifier.weight(1f))
            MetricCard(stringResource(Res.string.sales_metric), stringResource(Res.string.zero_rupees), Icons.Default.ShoppingCart, Modifier.weight(1f))
        }
    }
    item {
        ModuleFeatureCard(
            title = stringResource(Res.string.future_module_title, stringResource(module.labelRes)),
            subtitle = stringResource(Res.string.future_module_subtitle),
            theme = theme,
        )
    }
}

@Composable
fun FarmModuleTabs(
    selectedModule: FarmModuleTab,
    onSelected: (FarmModuleTab) -> Unit,
    theme: ModuleThemeColors,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.radiusLarge),
        colors = CardDefaults.cardColors(containerColor = theme.surface),
    ) {
        TabRow(selectedTabIndex = selectedModule.ordinal, containerColor = Color.Transparent, contentColor = theme.primary) {
            FarmModuleTab.entries.forEach { module ->
                val tabTheme = module.themeColors()
                Tab(
                    selected = selectedModule == module,
                    onClick = { onSelected(module) },
                    text = { Text(stringResource(module.labelRes), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(module.icon, contentDescription = stringResource(module.labelRes)) },
                    selectedContentColor = tabTheme.primary,
                    unselectedContentColor = tabTheme.primary.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
fun AgriNavigationTabs(
    selectedTab: FarmTab,
    onSelected: (FarmTab) -> Unit,
    theme: ModuleThemeColors,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space6, Alignment.CenterHorizontally),
        modifier = modifier.fillMaxWidth(),
    ) {
        FarmTab.entries.forEach { tab ->
            AgriMenuIcon(
                tab = tab,
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                theme = theme,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun AgriMenuIcon(
    tab: FarmTab,
    selected: Boolean,
    onClick: () -> Unit,
    theme: ModuleThemeColors,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) tab.containerColor else tab.containerColor
    val contentColor = if (selected) theme.onPrimaryContainer else tab.color

    Card(
        onClick = onClick,
        modifier = modifier.height(AppDimens.submenuHeight),
        shape = RoundedCornerShape(AppDimens.radiusSmall),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) AppDimens.space6 else AppDimens.space2),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimens.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(tab.icon, contentDescription = stringResource(tab.labelRes), modifier = Modifier.size(AppDimens.iconMedium), tint = tab.color)
            Spacer(Modifier.height(AppDimens.space3))
            Text(
                text = stringResource(tab.labelRes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ModuleFeatureCard(title: String, subtitle: String, theme: ModuleThemeColors) {
    Card(colors = CardDefaults.cardColors(containerColor = theme.primaryContainer, contentColor = theme.onPrimaryContainer)) {
        Column(Modifier.padding(AppDimens.space16), verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = theme.onPrimaryContainer.copy(alpha = 0.78f))
        }
    }
}

@Composable
fun SectionTotalCard(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    icon: ImageVector,
    tab: FarmTab,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = tab.containerColor, contentColor = tab.color),
        shape = RoundedCornerShape(AppDimens.radiusLarge),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.space14),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AppDimens.iconLarge))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimens.space2)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(primaryValue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            Text(secondaryValue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun CropDialog(initial: CropEntity? = null, onDismiss: () -> Unit, onSave: (CropEntity) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var variety by remember(initial) { mutableStateOf(initial?.variety.orEmpty()) }
    var field by remember(initial) { mutableStateOf(initial?.fieldName.orEmpty()) }
    var area by remember(initial) { mutableStateOf(initial?.area?.toString().orEmpty()) }
    var season by remember(initial) { mutableStateOf(initial?.season.orEmpty()) }
    val date = initial?.sowingDate ?: today()
    FormDialog(if (initial == null) stringResource(Res.string.dialog_add_crop) else stringResource(Res.string.dialog_edit_crop), onDismiss, onConfirm = {
        onSave(CropEntity(id = initial?.id ?: 0, name = name, variety = variety, fieldName = field, area = area.toDoubleOrZero(), season = season, sowingDate = date, notes = initial?.notes.orEmpty()))
    }) {
        AppTextField(name, { name = it }, stringResource(Res.string.field_crop_name))
        AppTextField(variety, { variety = it }, stringResource(Res.string.field_variety))
        AppTextField(field, { field = it }, stringResource(Res.string.field_field_name))
        AppTextField(area, { area = it }, stringResource(Res.string.field_area_acres))
        AppTextField(season, { season = it }, stringResource(Res.string.field_season))
        ReadOnlyTextField(date, stringResource(Res.string.field_sowing_date))
    }
}

@Composable
fun ExpenseDialog(crops: List<CropEntity>, initial: ExpenseEntity? = null, onDismiss: () -> Unit, onSave: (ExpenseEntity) -> Unit) {
    val defaultCategory = stringResource(Res.string.expense_category_seed)
    var crop by remember(initial, crops) { mutableStateOf(crops.firstOrNull { it.id == initial?.cropId } ?: crops.firstOrNull()) }
    var category by remember(initial) { mutableStateOf(initial?.category ?: defaultCategory) }
    var round by remember(initial) { mutableStateOf(initial?.applicationRound?.toString().orEmpty()) }
    var amount by remember(initial) { mutableStateOf(initial?.amount?.toString().orEmpty()) }
    val date = initial?.expenseDate ?: today()
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    FormDialog(if (initial == null) stringResource(Res.string.dialog_add_expense) else stringResource(Res.string.dialog_edit_expense), onDismiss, onConfirm = {
        crop?.let {
            onSave(ExpenseEntity(id = initial?.id ?: 0, cropId = it.id, category = category, applicationRound = round.toIntOrNull(), amount = amount.toDoubleOrZero(), expenseDate = date, notes = notes))
        }
    }) {
        CropSelector(crops, crop) { crop = it }
        CategorySelector(category) { category = it }
        AppTextField(round, { round = it }, stringResource(Res.string.field_application_round))
        AppTextField(amount, { amount = it }, stringResource(Res.string.field_amount))
        ReadOnlyTextField(date, stringResource(Res.string.field_expense_date))
        AppTextField(notes, { notes = it }, stringResource(Res.string.field_notes))
    }
}

@Composable
fun HarvestDialog(crops: List<CropEntity>, initial: HarvestEntity? = null, onDismiss: () -> Unit, onSave: (HarvestEntity) -> Unit) {
    var crop by remember(initial, crops) { mutableStateOf(crops.firstOrNull { it.id == initial?.cropId } ?: crops.firstOrNull()) }
    var kg by remember(initial) { mutableStateOf(initial?.quantityKg?.toString().orEmpty()) }
    val date = initial?.harvestDate ?: today()
    var notes by remember(initial) { mutableStateOf(initial?.managementNotes.orEmpty()) }
    FormDialog(if (initial == null) stringResource(Res.string.dialog_add_harvest) else stringResource(Res.string.dialog_edit_harvest), onDismiss, onConfirm = {
        crop?.let { onSave(HarvestEntity(id = initial?.id ?: 0, cropId = it.id, harvestDate = date, quantityKg = kg.toDoubleOrZero(), managementNotes = notes)) }
    }) {
        CropSelector(crops, crop) { crop = it }
        AppTextField(kg, { kg = it }, stringResource(Res.string.field_harvest_quantity))
        ReadOnlyTextField(date, stringResource(Res.string.field_harvest_date))
        AppTextField(notes, { notes = it }, stringResource(Res.string.field_management_notes))
    }
}

@Composable
fun SaleDialog(crops: List<CropEntity>, initial: SaleEntity? = null, onDismiss: () -> Unit, onSave: (SaleEntity) -> Unit) {
    var crop by remember(initial, crops) { mutableStateOf(crops.firstOrNull { it.id == initial?.cropId } ?: crops.firstOrNull()) }
    var kg by remember(initial) { mutableStateOf(initial?.quantityKg?.toString().orEmpty()) }
    var price by remember(initial) { mutableStateOf(initial?.pricePerKg?.toString().orEmpty()) }
    var buyer by remember(initial) { mutableStateOf(initial?.buyerName.orEmpty()) }
    var phone by remember(initial) { mutableStateOf(initial?.buyerPhone.orEmpty()) }
    val date = initial?.saleDate ?: today()
    FormDialog(if (initial == null) stringResource(Res.string.dialog_add_sale) else stringResource(Res.string.dialog_edit_sale), onDismiss, onConfirm = {
        crop?.let { onSave(SaleEntity(id = initial?.id ?: 0, cropId = it.id, saleDate = date, quantityKg = kg.toDoubleOrZero(), pricePerKg = price.toDoubleOrZero(), buyerName = buyer, buyerPhone = phone, notes = initial?.notes.orEmpty())) }
    }) {
        CropSelector(crops, crop) { crop = it }
        AppTextField(kg, { kg = it }, stringResource(Res.string.field_quantity_kg))
        AppTextField(price, { price = it }, stringResource(Res.string.field_price_per_kg))
        AppTextField(buyer, { buyer = it }, stringResource(Res.string.field_buyer_name))
        AppTextField(phone, { phone = it }, stringResource(Res.string.field_buyer_phone))
        ReadOnlyTextField(date, stringResource(Res.string.field_sale_date))
    }
}

@Composable
fun FormDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space10), content = content) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
fun CropSelector(crops: List<CropEntity>, selected: CropEntity?, onSelected: (CropEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.name ?: stringResource(Res.string.select_crop))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            crops.forEach { crop ->
                DropdownMenuItem(text = { Text(crop.name) }, onClick = { onSelected(crop); expanded = false })
            }
        }
    }
}

@Composable
fun CategorySelector(selected: String, onSelected: (String) -> Unit) {
    val categories = expenseCategories
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category) }, onClick = { onSelected(category); expanded = false })
            }
        }
    }
}

@Composable
fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
fun ReadOnlyTextField(value: String, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        singleLine = true,
    )
}

@Composable
fun HeroCard(title: String, value: String, subtitle: String, colors: List<Color>) {
    Column(
        Modifier
            .clip(RoundedCornerShape(AppDimens.radiusHero))
            .background(Brush.linearGradient(colors))
            .fillMaxWidth()
            .padding(AppDimens.space20),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text(subtitle, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(AppDimens.space16), verticalArrangement = Arrangement.spacedBy(AppDimens.space4), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
    if (onClick == null) {
        Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), content = cardContent)
    } else {
        Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), content = cardContent)
    }
}

@Composable
fun SummaryCard(summary: CropSummary) {
    DetailCard(
        title = summary.cropName,
        subtitle = stringResource(Res.string.summary_subtitle_format, formatKg(summary.harvestedKg), rupees(summary.totalIncome), rupees(summary.totalExpenses)),
        value = rupees(summary.profitLoss),
    )
}

@Composable
fun DetailCard(title: String, subtitle: String, value: String, onClick: (() -> Unit)? = null) {
    val content: @Composable ColumnScope.() -> Unit = {
        Row(Modifier.fillMaxWidth().padding(horizontal = AppDimens.space12, vertical = AppDimens.space10), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimens.space2)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (onClick == null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(AppDimens.space2), content = content)
    } else {
        Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(AppDimens.space2), content = content)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = AppDimens.space8))
}

fun monthLabel(month: Int?): String = MonthOptions.firstOrNull { it.number == month }?.label ?: "All Months"

fun periodLabel(month: Int?, year: String): String = if (month == null) year else "${monthLabel(month)} $year"

fun String.toDoubleOrZero(): Double = toDoubleOrNull() ?: 0.0

fun <T> List<T>.filterByPeriod(month: Int?, year: String, dateSelector: (T) -> String): List<T> {
    val key = if (month == null) year else "$year-${month.toString().padStart(2, '0')}"
    return filter { dateSelector(it).startsWith(key) }
}

fun rupees(value: Double): String = "Rs. ${"%,.2f".format(value)}"

fun formatKg(value: Double): String = "${"%,.2f".format(value)} kg"

