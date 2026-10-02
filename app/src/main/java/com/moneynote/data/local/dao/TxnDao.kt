package com.moneynote.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.moneynote.data.local.entity.Txn
import kotlinx.coroutines.flow.Flow

@Dao
interface TxnDao {

    @Query("SELECT * FROM txns ORDER BY dateMillis DESC, id DESC")
    fun observeAll(): Flow<List<Txn>>

    @Query(
        "SELECT * FROM txns WHERE dateMillis >= :start AND dateMillis < :end " +
            "ORDER BY dateMillis DESC, id DESC"
    )
    fun observeBetween(start: Long, end: Long): Flow<List<Txn>>

    @Query("SELECT * FROM txns WHERE id = :id")
    suspend fun findById(id: Long): Txn?

    @Insert
    suspend fun insert(txn: Txn): Long

    @Update
    suspend fun update(txn: Txn)

    @Delete
    suspend fun delete(txn: Txn)

    @Query("DELETE FROM txns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM txns")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM txns")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM txns WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int
}
