package com.example.ui.model

import com.example.data.model.ExpenseCategory

data class MonthlyTrendPoint(
    val yearMonth: String,
    val label: String,
    val totalExpense: Double,
    val totalIncome: Double,
    val budgetLimit: Double
) {
    val netSavings: Double get() = totalIncome - totalExpense
    val savingsRate: Double get() = if (totalIncome > 0) (netSavings / totalIncome) * 100 else 0.0
}

data class CategorySpending(
    val category: ExpenseCategory,
    val amount: Double,
    val percentage: Float, // 0f - 100f
    val transactionCount: Int,
    val budgetLimit: Double
) {
    val budgetPercentage: Float get() = if (budgetLimit > 0) ((amount / budgetLimit) * 100).toFloat() else 0f
    val isOverBudget: Boolean get() = budgetLimit > 0 && amount > budgetLimit
    val remainingBudget: Double get() = (budgetLimit - amount).coerceAtLeast(0.0)
}

data class DailySpending(
    val day: Int,
    val amount: Double,
    val cumulativeAmount: Double,
    val dateMillis: Long
)

data class DayOfWeekSpending(
    val dayName: String,
    val dayIndex: Int,
    val totalAmount: Double,
    val transactionCount: Int
)

enum class InsightType {
    SUCCESS,
    ALERT,
    TREND,
    INFO
}

data class SpendingInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val highlight: String = ""
)

enum class AppTab(val label: String) {
    DASHBOARD("Overview"),
    TRENDS("Trends & Charts"),
    BUDGETS("Budgets"),
    TRANSACTIONS("History")
}
