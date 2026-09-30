package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.ui.components.BudgetSummaryCard
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.InsightsBanner
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.MonthlyTrendChart
import com.example.ui.components.TransactionItemRow
import com.example.ui.model.CategorySpending
import com.example.ui.model.DailySpending
import com.example.ui.model.MonthlyTrendPoint
import com.example.ui.model.SpendingInsight

@Composable
fun HomeScreen(
    selectedMonth: String,
    totalSpent: Double,
    totalBudget: Double,
    totalIncome: Double,
    trends: List<MonthlyTrendPoint>,
    dailySpendings: List<DailySpending>,
    categorySpendings: List<CategorySpending>,
    recentTransactions: List<TransactionEntity>,
    insights: List<SpendingInsight>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditBudget: () -> Unit,
    onAddTransaction: () -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onNavigateToTrends: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    val filteredTransactions = remember(recentTransactions, selectedCategoryFilter) {
        if (selectedCategoryFilter != null) {
            recentTransactions.filter { it.expenseCategory == selectedCategoryFilter }
        } else {
            recentTransactions
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector
            item {
                MonthSelectorHeader(
                    currentYearMonth = selectedMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth
                )
            }

            // Budget Summary Card
            item {
                BudgetSummaryCard(
                    totalSpent = totalSpent,
                    totalBudget = totalBudget,
                    totalIncome = totalIncome,
                    onEditBudget = onEditBudget
                )
            }

            // Smart Insights Banner
            if (insights.isNotEmpty()) {
                item {
                    InsightsBanner(insights = insights)
                }
            }

            // Spending Trends Interactive Chart
            item {
                MonthlyTrendChart(
                    trends = trends,
                    dailySpendings = dailySpendings,
                    overallBudget = totalBudget
                )
            }

            // Category Donut Chart
            item {
                CategoryDonutChart(
                    categorySpendings = categorySpendings,
                    totalSpent = totalSpent,
                    selectedCategory = selectedCategoryFilter,
                    onCategorySelected = { selectedCategoryFilter = it }
                )
            }

            // Recent Transactions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedCategoryFilter != null) "${selectedCategoryFilter?.title} Transactions" else "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedCategoryFilter != null) "Filtered by selected category" else "Latest records this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.testTag("see_all_transactions_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "See All",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Transactions List
            if (filteredTransactions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredTransactions.take(6),
                    key = { it.id }
                ) { txn ->
                    TransactionItemRow(
                        transaction = txn,
                        onDelete = { onDeleteTransaction(txn.id) }
                    )
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddTransaction,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 20.dp)
                .testTag("add_transaction_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Transaction",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
