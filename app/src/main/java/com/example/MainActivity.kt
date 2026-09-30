package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.dialogs.AddTransactionSheet
import com.example.ui.dialogs.EditBudgetDialog
import com.example.ui.model.AppTab
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.TrendsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.DateUtils

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SpendWiseApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendWiseApp(viewModel: ExpenseViewModel) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }

    // Collect Reactive State
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val totalSpent by viewModel.totalExpense.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val currentBudgetEntity by viewModel.currentBudget.collectAsStateWithLifecycle()
    val totalBudget = currentBudgetEntity?.overallBudget ?: 3500.0

    val currentMonthTransactions by viewModel.currentMonthTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val categorySpendings by viewModel.categorySpendings.collectAsStateWithLifecycle()
    val trends by viewModel.multiMonthTrends.collectAsStateWithLifecycle()
    val dailySpendings by viewModel.dailySpendings.collectAsStateWithLifecycle()
    val dayOfWeekSpendings by viewModel.dayOfWeekSpendings.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()

    // Handle back button when not on dashboard
    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        currentTab = AppTab.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SpendWise",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Budget & Spending Trends",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("current_month_badge")
                    ) {
                        Text(
                            text = DateUtils.formatShortMonth(selectedMonth),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // Tab 1: Dashboard
                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { currentTab = AppTab.DASHBOARD },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Overview"
                        )
                    },
                    label = { Text("Overview", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_overview")
                )

                // Tab 2: Trends & Charts
                NavigationBarItem(
                    selected = currentTab == AppTab.TRENDS,
                    onClick = { currentTab = AppTab.TRENDS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.TRENDS) Icons.Default.ShowChart else Icons.Outlined.ShowChart,
                            contentDescription = "Trends"
                        )
                    },
                    label = { Text("Trends", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_trends")
                )

                // Tab 3: Budgets
                NavigationBarItem(
                    selected = currentTab == AppTab.BUDGETS,
                    onClick = { currentTab = AppTab.BUDGETS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.BUDGETS) Icons.Default.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Budgets"
                        )
                    },
                    label = { Text("Budgets", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_budgets")
                )

                // Tab 4: History
                NavigationBarItem(
                    selected = currentTab == AppTab.TRANSACTIONS,
                    onClick = { currentTab = AppTab.TRANSACTIONS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.TRANSACTIONS) Icons.Default.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "tab_content_transition"
        ) { tab ->
            when (tab) {
                AppTab.DASHBOARD -> HomeScreen(
                    selectedMonth = selectedMonth,
                    totalSpent = totalSpent,
                    totalBudget = totalBudget,
                    totalIncome = totalIncome,
                    trends = trends,
                    dailySpendings = dailySpendings,
                    categorySpendings = categorySpendings,
                    recentTransactions = currentMonthTransactions,
                    insights = insights,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onEditBudget = { showEditBudgetDialog = true },
                    onAddTransaction = { showAddTransactionSheet = true },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                    onNavigateToTrends = { currentTab = AppTab.TRENDS },
                    onNavigateToTransactions = { currentTab = AppTab.TRANSACTIONS }
                )

                AppTab.TRENDS -> TrendsScreen(
                    selectedMonth = selectedMonth,
                    totalSpent = totalSpent,
                    totalBudget = totalBudget,
                    totalIncome = totalIncome,
                    trends = trends,
                    dailySpendings = dailySpendings,
                    categorySpendings = categorySpendings,
                    dayOfWeekSpendings = dayOfWeekSpendings,
                    currentMonthTransactions = currentMonthTransactions,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                )

                AppTab.BUDGETS -> BudgetsScreen(
                    selectedMonth = selectedMonth,
                    totalSpent = totalSpent,
                    totalBudget = totalBudget,
                    totalIncome = totalIncome,
                    categorySpendings = categorySpendings,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onEditOverallBudget = { showEditBudgetDialog = true },
                    onEditCategoryBudget = { cat, amt -> viewModel.updateCategoryBudget(cat, amt) },
                    onResetData = { viewModel.resetData() }
                )

                AppTab.TRANSACTIONS -> TransactionsScreen(
                    allTransactions = allTransactions,
                    onAddTransaction = { showAddTransactionSheet = true },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) }
                )
            }
        }
    }

    // Add Transaction Bottom Sheet
    if (showAddTransactionSheet) {
        AddTransactionSheet(
            onDismiss = { showAddTransactionSheet = false },
            onSave = { title, amount, category, type, timestamp, notes, paymentMethod ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    category = category,
                    type = type,
                    timestamp = timestamp,
                    notes = notes,
                    paymentMethod = paymentMethod
                )
                showAddTransactionSheet = false
            }
        )
    }

    // Edit Budget Dialog
    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentBudget = totalBudget,
            onDismiss = { showEditBudgetDialog = false },
            onSave = { amount ->
                viewModel.updateOverallBudget(amount)
                showEditBudgetDialog = false
            }
        )
    }
}
