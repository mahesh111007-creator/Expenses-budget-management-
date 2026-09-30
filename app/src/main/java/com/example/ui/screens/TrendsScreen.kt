package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.MonthlyTrendChart
import com.example.ui.components.TransactionItemRow
import com.example.ui.model.CategorySpending
import com.example.ui.model.DailySpending
import com.example.ui.model.DayOfWeekSpending
import com.example.ui.model.MonthlyTrendPoint
import kotlin.math.max

@Composable
fun TrendsScreen(
    selectedMonth: String,
    totalSpent: Double,
    totalBudget: Double,
    totalIncome: Double,
    trends: List<MonthlyTrendPoint>,
    dailySpendings: List<DailySpending>,
    categorySpendings: List<CategorySpending>,
    dayOfWeekSpendings: List<DayOfWeekSpending>,
    currentMonthTransactions: List<TransactionEntity>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    val topExpenses = remember(currentMonthTransactions) {
        currentMonthTransactions
            .filter { it.transactionType == TransactionType.EXPENSE }
            .sortedByDescending { it.amount }
            .take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trends_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Header
        item {
            MonthSelectorHeader(
                currentYearMonth = selectedMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
        }

        // Analytical Metric Highlight Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val avg6m = if (trends.isNotEmpty()) trends.map { it.totalExpense }.average() else 0.0
                StatPillCard(
                    title = "6-Mo Average",
                    value = "$%.0f".format(avg6m),
                    subtitle = "Monthly spending",
                    modifier = Modifier.weight(1f)
                )

                val highestMonth = trends.maxByOrNull { it.totalExpense }
                StatPillCard(
                    title = "Peak Month",
                    value = highestMonth?.label ?: "N/A",
                    subtitle = if (highestMonth != null) "$%.0f".format(highestMonth.totalExpense) else "-",
                    modifier = Modifier.weight(1f)
                )

                val savingsRate = if (totalIncome > 0) ((totalIncome - totalSpent) / totalIncome) * 100 else 0.0
                StatPillCard(
                    title = "Savings Rate",
                    value = "%.1f%%".format(savingsRate),
                    subtitle = if (savingsRate >= 0) "Surplus" else "Deficit",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Primary Monthly Spending Trends interactive chart
        item {
            MonthlyTrendChart(
                trends = trends,
                dailySpendings = dailySpendings,
                overallBudget = totalBudget
            )
        }

        // Category Breakdown interactive Donut chart
        item {
            CategoryDonutChart(
                categorySpendings = categorySpendings,
                totalSpent = totalSpent,
                selectedCategory = selectedCategoryFilter,
                onCategorySelected = { selectedCategoryFilter = it }
            )
        }

        // Category Detailed Ranking with Budgets
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_ranking_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Category Ranking & Pacing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Actual spend vs category budget allocations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    categorySpendings.forEach { item ->
                        val isOver = item.isOverBudget
                        val progress = if (item.budgetLimit > 0) (item.amount / item.budgetLimit).toFloat().coerceIn(0f, 1f) else 0f

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(item.category.composeColor.copy(alpha = 0.18f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.category.icon,
                                            contentDescription = null,
                                            tint = item.category.composeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.category.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$%.2f".format(item.amount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item.budgetLimit > 0) {
                                        Text(
                                            text = " / $%.0f".format(item.budgetLimit),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Progress
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (isOver) MaterialTheme.colorScheme.error else item.category.composeColor
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Day of Week Distribution Chart
        item {
            DayOfWeekDistributionCard(spendings = dayOfWeekSpendings)
        }

        // Top 5 Highest Expenses
        if (topExpenses.isNotEmpty()) {
            item {
                Text(
                    text = "Top Expenses This Month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(topExpenses, key = { it.id }) { txn ->
                TransactionItemRow(
                    transaction = txn,
                    onDelete = { onDeleteTransaction(txn.id) }
                )
            }
        }
    }
}

@Composable
private fun StatPillCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DayOfWeekDistributionCard(
    spendings: List<DayOfWeekSpending>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    val maxDayAmount = remember(spendings) {
        val mx = spendings.maxOfOrNull { it.totalAmount } ?: 100.0
        max(mx, 50.0) * 1.15
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(spendings) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(700))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("day_of_week_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Day-of-Week Spending Pattern",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Identify which days you spend the most",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val count = spendings.size
                    if (count == 0) return@Canvas

                    val chartWidth = size.width
                    val chartHeight = size.height - 24.dp.toPx()
                    val bottomY = chartHeight
                    val stepX = chartWidth / count
                    val barWidth = stepX * 0.52f

                    for (i in 0 until count) {
                        val item = spendings[i]
                        val centerX = (i * stepX) + (stepX / 2f)
                        val barHeight = ((item.totalAmount / maxDayAmount) * bottomY).toFloat() * animProgress.value
                        val topY = bottomY - barHeight

                        val isWeekend = item.dayIndex == 1 || item.dayIndex == 7
                        val barBrush = Brush.verticalGradient(
                            colors = if (isWeekend) {
                                listOf(secondaryColor, secondaryColor.copy(alpha = 0.5f))
                            } else {
                                listOf(primaryColor, primaryColor.copy(alpha = 0.4f))
                            },
                            startY = topY,
                            endY = bottomY
                        )

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(centerX - barWidth / 2f, topY),
                            size = Size(barWidth, barHeight.coerceAtLeast(3.dp.toPx())),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                // Day Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    spendings.forEach { item ->
                        Text(
                            text = item.dayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Peak Day Insight
            val peakDay = spendings.maxByOrNull { it.totalAmount }
            if (peakDay != null && peakDay.totalAmount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Peak spending on ${peakDay.dayName}s with $%.0f total spent.".format(peakDay.totalAmount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
