package com.devsgroup.foodmanager.core.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import com.devsgroup.foodmanager.core.database.entity.OutboxEntryEntity

@Dao
interface OutboxDao {

    @Insert
    suspend fun insert(entry: OutboxEntryEntity)

    /** Oldest first, so interactions are replayed to the server in the order they happened. */
    @Query("SELECT * FROM outbox_entries ORDER BY id ASC")
    suspend fun getAll(): List<OutboxEntryEntity>

    @Delete
    suspend fun delete(entry: OutboxEntryEntity)
}
