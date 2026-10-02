package com.moneynote.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.moneynote.data.local.entity.Category
import com.moneynote.data.model.TxnType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY type ASC, sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<Category>>

    @Query(
        "SELECT * FROM categories WHERE archived = 0 AND type = :type " +
            "ORDER BY sortOrder ASC, id ASC"
    )
    fun observeByType(type: TxnType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun observeById(id: Long): Flow<Category?>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: Long): Category?

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("UPDATE categories SET archived = 1 WHERE id = :id")
    suspend fun archiveById(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM categories WHERE type = :type")
    suspend fun nextSortOrder(type: TxnType): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
