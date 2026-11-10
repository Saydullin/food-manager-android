package com.devsgroup.foodmanager.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.devsgroup.foodmanager.core.database.entity.EaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EaterDao {

    /** Visible household members, in the order they were added. */
    @Query("SELECT * FROM eaters WHERE deleted = 0 ORDER BY createdAt ASC, id ASC")
    fun observeVisible(): Flow<List<EaterEntity>>

    @Query("SELECT * FROM eaters WHERE id = :id")
    suspend fun get(id: String): EaterEntity?

    /** Rows whose local state still has to reach the server (upserts and tombstones). */
    @Query("SELECT * FROM eaters WHERE pendingSync = 1")
    suspend fun pending(): List<EaterEntity>

    @Upsert
    suspend fun upsert(eater: EaterEntity)

    /** Settles a push — unless a newer local edit bumped [EaterEntity.localVersion] meanwhile. */
    @Query("UPDATE eaters SET pendingSync = 0 WHERE id = :id AND localVersion = :version")
    suspend fun markSynced(id: String, version: Long): Int

    @Query("DELETE FROM eaters WHERE id = :id")
    suspend fun delete(id: String)

    /** Only removes a tombstone that wasn't revived by a newer edit. */
    @Query("DELETE FROM eaters WHERE id = :id AND deleted = 1 AND localVersion = :version")
    suspend fun deleteTombstone(id: String, version: Long)

    @Query("DELETE FROM eaters WHERE pendingSync = 0")
    suspend fun deleteSynced()

    /**
     * Adopts the server's list: every settled row is replaced by the server's copy,
     * while rows with unsynced local edits are left alone — the server copy predates them.
     */
    @Transaction
    suspend fun replaceSynced(server: List<EaterEntity>) {
        val pendingIds = pending().mapTo(HashSet()) { it.id }
        deleteSynced()
        server.filter { it.id !in pendingIds }.forEach { upsert(it) }
    }

    /** Wipes the household on logout — it belongs to the signed-out account. */
    @Query("DELETE FROM eaters")
    suspend fun clear()
}
