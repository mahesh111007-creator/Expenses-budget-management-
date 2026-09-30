package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.ExpenseRepository
import com.example.ui.model.CategorySpending
import com.example.ui.model.DailySpending
import com.example.ui.model.DayOfWeekSpending
import com.example.ui.model.InsightType
import com.example.ui.model.MonthlyTrendPoint
import com.example.ui.model.SpendingInsight
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(db.transactionDao(), db.budgetDao())
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureInitialDataLoaded()
        }
    }

    private val _selectedMonth = MutableStateFlow(DateUtils.getCurrentYearMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // All transactions for search/history tab
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions for currently selected month
    val currentMonthTransactions: StateFlow<List<TransactionEntity>> = _selectedMonth
        .flatMapLatest { month -> repository.getTransactionsForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget for currently selected month
    val currentBudget: StateFlow<MonthlyBudgetEntity?> = _selectedMonth
        .flatMapLatest { month -> repository.getBudgetForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Summary calculations
    val totalExpense: StateFlow<Double> = currentMonthTransactions.combine(allTransactions) { list, _ ->
        list.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = currentMonthTransactions.combine(allTransactions) { list, _ ->
        list.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category Spendings breakdown
    val categorySpendings: StateFlow<List<CategorySpending>> = combine(
        currentMonthTransactions,
        currentBudget
    ) { transactions, budgetEntity ->
        val expenseTxns = transactions.filter { it.transactionType == TransactionType.EXPENSE }
        val totalSpent = expenseTxns.sumOf { it.amount }
        val categoryBudgets = budgetEntity?.getCategoryBudgets() ?: emptyMap()

        val grouped = expenseTxns.groupBy { it.expenseCategory }

        ExpenseCategory.getExpenseCategories().mapNotNull { cat ->
            val items = grouped[cat] ?: emptyList()
            val sum = items.sumOf { it.amount }
            val count = items.size
            val budget = categoryBudgets[cat.name] ?: cat.defaultBudget

            if (sum > 0 || budget > 0) {
                val percentage = if (totalSpent > 0) ((sum / totalSpent) * 100).toFloat() else 0f
                CategorySpending(
                    category = cat,
                    amount = sum,
                    percentage = percentage,
                    transactionCount = count,
                    budgetLimit = budget
                )
            } else null
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-month trends (last 6 months)
    val multiMonthTrends: StateFlow<List<MonthlyTrendPoint>> = combine(
        allTransactions,
        _selectedMonth
    ) { allTxns, currentMonthKey ->
        val months = DateUtils.getLastNMonths(6, currentMonthKey)
        months.map { monthKey ->
            val (start, end) = DateUtils.getMonthRangeMillis(monthKey)
            val monthTxns = allTxns.filter { it.timestamp in start..end }
            val expense = monthTxns.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
            val income = monthTxns.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
            val budgetEntity = repository.getBudgetForMonthSync(monthKey)
            val budgetLimit = budgetEntity?.overallBudget ?: 3500.0

            MonthlyTrendPoint(
                yearMonth = monthKey,
                label = DateUtils.formatShortMonth(monthKey),
                totalExpense = expense,
                totalIncome = income,
                budgetLimit = budgetLimit
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily cumulative spending for current month
    val dailySpendings: StateFlow<List<DailySpending>> = combine(
        currentMonthTransactions,
        _selectedMonth
    ) { transactions, monthKey ->
        val daysInMonth = DateUtils.getDaysInMonth(monthKey)
        val expenseTxns = transactions.filter { it.transactionType == TransactionType.EXPENSE }
        val (start, _) = DateUtils.getMonthRangeMillis(monthKey)

        val cal = Calendar.getInstance()
        cal.timeInMillis = start

        val dailyList = mutableListOf<DailySpending>()
        var runningCumulative = 0.0

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dayEnd = cal.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val daySpent = expenseTxns
                .filter { it.timestamp in dayStart..dayEnd }
                .sumOf { it.amount }

            runningCumulative += daySpent

            dailyList.add(
                DailySpending(
                    day = day,
                    amount = daySpent,
                    cumulativeAmount = runningCumulative,
                    dateMillis = dayStart
                )
            )
        }
        dailyList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Day of week spending patterns
    val dayOfWeekSpendings: StateFlow<List<DayOfWeekSpending>> = currentMonthTransactions.combine(allTransactions) { txns, _ ->
        val expenseTxns = txns.filter { it.transactionType == TransactionType.EXPENSE }
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        val grouped = expenseTxns.groupBy { DateUtils.getDayOfWeekIndex(it.timestamp) }

        (1..7).map { dayIdx ->
            val items = grouped[dayIdx] ?: emptyList()
            DayOfWeekSpending(
                dayName = dayNames[dayIdx - 1],
                dayIndex = dayIdx,
                totalAmount = items.sumOf { it.amount },
                transactionCount = items.size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Spending insights
    val insights: StateFlow<List<SpendingInsight>> = combine(
        currentMonthTransactions,
        categorySpendings,
        currentBudget,
        multiMonthTrends
    ) { txns, categories, budget, trends ->
        val list = mutableListOf<SpendingInsight>()
        val totalSpent = txns.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
        val budgetLimit = budget?.overallBudget ?: 3500.0

        // Month over month trend comparison
        if (trends.size >= 2) {
            val currMonth = trends.last()
            val prevMonth = trends[trends.size - 2]
            if (prevMonth.totalExpense > 0) {
                val diffPercent = ((currMonth.totalExpense - prevMonth.totalExpense) / prevMonth.totalExpense) * 100
                if (diffPercent < 0) {
                    list.add(
                        SpendingInsight(
                            type = InsightType.SUCCESS,
                            title = "Spending Down %.1f%%".format(kotlin.math.abs(diffPercent)),
                            description = "You've spent $%.0f less than at this point last month. Great pacing!".format(prevMonth.totalExpense - currMonth.totalExpense),
                            highlight = "-%.1f%%".format(kotlin.math.abs(diffPercent))
                        )
                    )
                } else if (diffPercent > 10) {
                    list.add(
                        SpendingInsight(
                            type = InsightType.ALERT,
                            title = "Pacing %.1f%% Higher".format(diffPercent),
                            description = "Spending is higher than ${prevMonth.label}. Check top categories to trim back.",
                            highlight = "+%.1f%%".format(diffPercent)
                        )
                    )
                }
            }
        }

        // Budget burn rate
        val remaining = (budgetLimit - totalSpent).coerceAtLeast(0.0)
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = (daysInMonth - currentDay).coerceAtLeast(1)
        val dailyAllowance = remaining / daysLeft

        if (totalSpent > budgetLimit) {
            list.add(
                SpendingInsight(
                    type = InsightType.ALERT,
                    title = "Monthly Budget Exceeded",
                    description = "You are $%.2f over your $%.0f budget limit for this month.".format(totalSpent - budgetLimit, budgetLimit),
                    highlight = "Over Budget"
                )
            )
        } else {
            list.add(
                SpendingInsight(
                    type = InsightType.INFO,
                    title = "Daily Safe Spend: $%.0f/day".format(dailyAllowance),
                    description = "$%.0f left across remaining %d days this month.".format(remaining, daysLeft),
                    highlight = "$%.0f/day".format(dailyAllowance)
                )
            )
        }

        // Top Category insight
        categories.maxByOrNull { it.amount }?.let { top ->
            if (top.amount > 0) {
                list.add(
                    SpendingInsight(
                        type = InsightType.TREND,
                        title = "Top Category: ${top.category.title}",
                        description = "Accounts for %.1f%% ($%.0f) of your expenses this month.".format(top.percentage, top.amount),
                        highlight = "%.0f%%".format(top.percentage)
                    )
                )
            }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedMonth(yearMonth: String) {
        _selectedMonth.value = yearMonth
    }

    fun previousMonth() {
        _selectedMonth.value = DateUtils.getPreviousMonth(_selectedMonth.value)
    }

    fun nextMonth() {
        _selectedMonth.value = DateUtils.getNextMonth(_selectedMonth.value)
    }

    fun addTransaction(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        type: TransactionType,
        timestamp: Long,
        notes: String,
        paymentMethod: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = TransactionEntity(
                title = title.trim(),
                amount = amount,
                category = category.name,
                type = type.name,
                timestamp = timestamp,
                notes = notes.trim(),
                paymentMethod = paymentMethod
            )
            repository.insertTransaction(entity)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransactionById(id)
        }
    }

    fun updateOverallBudget(amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getBudgetForMonthSync(_selectedMonth.value)
            val updated = current?.copy(overallBudget = amount) ?: MonthlyBudgetEntity(
                yearMonth = _selectedMonth.value,
                overallBudget = amount
            )
            repository.saveBudget(updated)
        }
    }

    fun updateCategoryBudget(category: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getBudgetForMonthSync(_selectedMonth.value)
            val existingBudgets = current?.getCategoryBudgets()?.toMutableMap() ?: mutableMapOf()
            existingBudgets[category] = amount
            val updated = current?.copy(
                categoryBudgetsJson = MonthlyBudgetEntity.createBudgetsJson(existingBudgets)
            ) ?: MonthlyBudgetEntity(
                yearMonth = _selectedMonth.value,
                overallBudget = 3500.0,
                categoryBudgetsJson = MonthlyBudgetEntity.createBudgetsJson(existingBudgets)
            )
            repository.saveBudget(updated)
        }
    }

    fun resetData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetWithSampleData()
        }
    }
}
