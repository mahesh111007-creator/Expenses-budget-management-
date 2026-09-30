package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MonthlyBudgetEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp DESC")
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startMillis AND timestamp <= :endMillis")
    suspend fun getTransactionsBetweenSync(startMillis: Long, endMillis: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth LIMIT 1")
    fun getBudgetForMonth(yearMonth: String): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth LIMIT 1")
    suspend fun getBudgetForMonthSync(yearMonth: String): MonthlyBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM monthly_budgets ORDER BY yearMonth DESC")
    fun getAllBudgets(): Flow<List<MonthlyBudgetEntity>>
}
