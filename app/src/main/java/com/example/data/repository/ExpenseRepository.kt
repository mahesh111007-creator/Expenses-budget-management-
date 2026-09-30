package com.example.data.repository

import com.example.data.local.BudgetDao
import com.example.data.local.TransactionDao
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Random

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    fun getTransactionsForMonth(yearMonth: String): Flow<List<TransactionEntity>> {
        val (start, end) = DateUtils.getMonthRangeMillis(yearMonth)
        return transactionDao.getTransactionsBetween(start, end)
    }

    suspend fun getTransactionsForMonthSync(yearMonth: String): List<TransactionEntity> {
        val (start, end) = DateUtils.getMonthRangeMillis(yearMonth)
        return transactionDao.getTransactionsBetweenSync(start, end)
    }

    fun getBudgetForMonth(yearMonth: String): Flow<MonthlyBudgetEntity?> =
        budgetDao.getBudgetForMonth(yearMonth)

    suspend fun getBudgetForMonthSync(yearMonth: String): MonthlyBudgetEntity? =
        budgetDao.getBudgetForMonthSync(yearMonth)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insert(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.update(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.delete(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteById(id)

    suspend fun saveBudget(budget: MonthlyBudgetEntity) =
        budgetDao.insertOrUpdate(budget)

    suspend fun ensureInitialDataLoaded() {
        val count = transactionDao.getCount()
        if (count == 0) {
            seedSampleData()
        }
    }

    suspend fun resetWithSampleData() {
        seedSampleData()
    }

    private suspend fun seedSampleData() {
        val transactions = mutableListOf<TransactionEntity>()
        val months = DateUtils.getLastNMonths(5, DateUtils.getCurrentYearMonth())
        val random = Random(42)

        for ((mIndex, month) in months.withIndex()) {
            val (start, end) = DateUtils.getMonthRangeMillis(month)
            val cal = Calendar.getInstance()
            cal.timeInMillis = start

            val daysInMonth = DateUtils.getDaysInMonth(month)

            // Monthly Salary Income
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            transactions.add(
                TransactionEntity(
                    title = "Monthly Salary Deposit",
                    amount = 4500.0,
                    category = ExpenseCategory.INCOME.name,
                    type = TransactionType.INCOME.name,
                    timestamp = cal.timeInMillis,
                    notes = "Direct deposit from employer",
                    paymentMethod = "Bank Transfer"
                )
            )

            // Rent / Housing
            cal.set(Calendar.DAY_OF_MONTH, 2)
            cal.set(Calendar.HOUR_OF_DAY, 10)
            transactions.add(
                TransactionEntity(
                    title = "Apartment Rent & Utilities",
                    amount = 1250.0,
                    category = ExpenseCategory.HOUSING.name,
                    type = TransactionType.EXPENSE.name,
                    timestamp = cal.timeInMillis,
                    notes = "Monthly lease payment",
                    paymentMethod = "Bank Transfer"
                )
            )

            // Recurring Bills
            cal.set(Calendar.DAY_OF_MONTH, 5)
            transactions.add(
                TransactionEntity(
                    title = "High-Speed Internet & Mobile",
                    amount = 115.0 + random.nextInt(15),
                    category = ExpenseCategory.BILLS.name,
                    type = TransactionType.EXPENSE.name,
                    timestamp = cal.timeInMillis,
                    notes = "Fiber & phone plan",
                    paymentMethod = "Credit Card"
                )
            )

            // Groceries spread over the month
            val groceryDays = listOf(3, 10, 17, 24)
            for (day in groceryDays) {
                if (day <= daysInMonth) {
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    cal.set(Calendar.HOUR_OF_DAY, 17)
                    transactions.add(
                        TransactionEntity(
                            title = listOf("Whole Foods Market", "Trader Joe's", "Costco Wholesale", "Local Farmers Market")[day % 4],
                            amount = 85.0 + random.nextInt(65),
                            category = ExpenseCategory.GROCERIES.name,
                            type = TransactionType.EXPENSE.name,
                            timestamp = cal.timeInMillis,
                            notes = "Weekly grocery restock",
                            paymentMethod = "Credit Card"
                        )
                    )
                }
            }

            // Food & Dining Out
            val diningDays = listOf(4, 8, 12, 16, 21, 27)
            for (day in diningDays) {
                if (day <= daysInMonth) {
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    cal.set(Calendar.HOUR_OF_DAY, 19)
                    transactions.add(
                        TransactionEntity(
                            title = listOf("Sushi Bistro", "Italian Trattoria", "Coffee & Pastries", "Artisan Burger Bar", "Thai Kitchen", "Taco Cantina")[day % 6],
                            amount = 25.0 + random.nextInt(75),
                            category = ExpenseCategory.FOOD.name,
                            type = TransactionType.EXPENSE.name,
                            timestamp = cal.timeInMillis,
                            notes = "Dinner with friends/coffee break",
                            paymentMethod = "Credit Card"
                        )
                    )
                }
            }

            // Transportation / Fuel / Transit
            val transportDays = listOf(7, 18, 26)
            for (day in transportDays) {
                if (day <= daysInMonth) {
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    cal.set(Calendar.HOUR_OF_DAY, 8)
                    transactions.add(
                        TransactionEntity(
                            title = listOf("Chevron Fuel Fill-up", "Metro Transit Pass", "Uber Ride", "Shell Gas Station")[day % 4],
                            amount = 35.0 + random.nextInt(40),
                            category = ExpenseCategory.TRANSPORT.name,
                            type = TransactionType.EXPENSE.name,
                            timestamp = cal.timeInMillis,
                            notes = "Commute & transit",
                            paymentMethod = "Debit Card"
                        )
                    )
                }
            }

            // Shopping / Entertainment / Health
            cal.set(Calendar.DAY_OF_MONTH, 11)
            transactions.add(
                TransactionEntity(
                    title = "Streaming Services & Cinema",
                    amount = 45.0,
                    category = ExpenseCategory.ENTERTAINMENT.name,
                    type = TransactionType.EXPENSE.name,
                    timestamp = cal.timeInMillis,
                    notes = "Netflix & movie tickets",
                    paymentMethod = "Digital Wallet"
                )
            )

            cal.set(Calendar.DAY_OF_MONTH, 14)
            transactions.add(
                TransactionEntity(
                    title = "Gym Membership & Yoga",
                    amount = 75.0,
                    category = ExpenseCategory.HEALTH.name,
                    type = TransactionType.EXPENSE.name,
                    timestamp = cal.timeInMillis,
                    notes = "Monthly health club fee",
                    paymentMethod = "Credit Card"
                )
            )

            cal.set(Calendar.DAY_OF_MONTH, 22)
            transactions.add(
                TransactionEntity(
                    title = "Apparel & Online Shopping",
                    amount = 95.0 + random.nextInt(80),
                    category = ExpenseCategory.SHOPPING.name,
                    type = TransactionType.EXPENSE.name,
                    timestamp = cal.timeInMillis,
                    notes = "Seasonal wardrobe refresh",
                    paymentMethod = "Credit Card"
                )
            )

            // Setup default budget for this month
            val budgetMap = mapOf(
                ExpenseCategory.HOUSING.name to 1300.0,
                ExpenseCategory.FOOD.name to 500.0,
                ExpenseCategory.GROCERIES.name to 450.0,
                ExpenseCategory.TRANSPORT.name to 250.0,
                ExpenseCategory.BILLS.name to 200.0,
                ExpenseCategory.ENTERTAINMENT.name to 180.0,
                ExpenseCategory.SHOPPING.name to 300.0,
                ExpenseCategory.HEALTH.name to 150.0
            )
            budgetDao.insertOrUpdate(
                MonthlyBudgetEntity(
                    yearMonth = month,
                    overallBudget = 3400.0,
                    categoryBudgetsJson = MonthlyBudgetEntity.createBudgetsJson(budgetMap)
                )
            )
        }

        transactionDao.insertAll(transactions)
    }
}
