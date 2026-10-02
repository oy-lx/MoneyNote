package com.moneynote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneynote.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun observeByMonth(yearMonth: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth AND categoryId = :categoryId LIMIT 1")
    suspend fun find(yearMonth: String, categoryId: Long): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Query("DELETE FROM budgets WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
    suspend fun delete(yearMonth: String, categoryId: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll(): Int
}
