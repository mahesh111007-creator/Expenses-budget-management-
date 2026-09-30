package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class ExpenseCategory(
    val title: String,
    val color: Long,
    val defaultBudget: Double
) {
    FOOD("Food & Dining", 0xFFF59E0B, 500.0),
    GROCERIES("Groceries", 0xFF10B981, 450.0),
    HOUSING("Housing & Rent", 0xFF6366F1, 1200.0),
    TRANSPORT("Transport", 0xFF3B82F6, 250.0),
    BILLS("Bills & Utilities", 0xFF06B6D4, 200.0),
    ENTERTAINMENT("Entertainment", 0xFFEC4899, 180.0),
    HEALTH("Health & Medical", 0xFF14B8A6, 150.0),
    SHOPPING("Shopping", 0xFF8B5CF6, 300.0),
    TRAVEL("Travel & Vacation", 0xFFF97316, 200.0),
    EDUCATION("Education", 0xFF38BDF8, 100.0),
    PERSONAL("Personal Care", 0xFFE11D48, 120.0),
    INCOME("Salary & Income", 0xFF22C55E, 0.0),
    OTHER("Other", 0xFF64748B, 150.0);

    val composeColor: Color
        get() = Color(color)

    val icon: ImageVector
        get() = when (this) {
            FOOD -> Icons.Default.Restaurant
            GROCERIES -> Icons.Default.ShoppingBag
            HOUSING -> Icons.Default.Home
            TRANSPORT -> Icons.Default.DirectionsCar
            BILLS -> Icons.Default.ReceiptLong
            ENTERTAINMENT -> Icons.Default.Movie
            HEALTH -> Icons.Default.FitnessCenter
            SHOPPING -> Icons.Default.ShoppingBag
            TRAVEL -> Icons.Default.CardTravel
            EDUCATION -> Icons.Default.School
            PERSONAL -> Icons.Default.Spa
            INCOME -> Icons.Default.AccountBalance
            OTHER -> Icons.Default.MoreHoriz
        }

    companion object {
        fun fromString(name: String): ExpenseCategory {
            return try {
                valueOf(name)
            } catch (e: Exception) {
                values().firstOrNull { it.title.equals(name, ignoreCase = true) } ?: OTHER
            }
        }

        fun getExpenseCategories(): List<ExpenseCategory> {
            return values().filter { it != INCOME }
        }
    }
}

val PAYMENT_METHODS = listOf(
    "Credit Card",
    "Debit Card",
    "Cash",
    "Bank Transfer",
    "Digital Wallet"
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String, // ExpenseCategory enum name
    val type: String = TransactionType.EXPENSE.name,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val paymentMethod: String = "Credit Card"
) {
    val expenseCategory: ExpenseCategory
        get() = ExpenseCategory.fromString(category)

    val transactionType: TransactionType
        get() = try {
            TransactionType.valueOf(type)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
}

@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetEntity(
    @PrimaryKey
    val yearMonth: String, // e.g. "2026-09"
    val overallBudget: Double = 3500.0,
    val categoryBudgetsJson: String = "" // key=val;key=val format
) {
    fun getCategoryBudgets(): Map<String, Double> {
        if (categoryBudgetsJson.isBlank()) return emptyMap()
        val result = mutableMapOf<String, Double>()
        categoryBudgetsJson.split(";").forEach { item ->
            val parts = item.split("=")
            if (parts.size == 2) {
                parts[1].toDoubleOrNull()?.let { amount ->
                    result[parts[0]] = amount
                }
            }
        }
        return result
    }

    companion object {
        fun createBudgetsJson(budgets: Map<String, Double>): String {
            return budgets.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }
}
