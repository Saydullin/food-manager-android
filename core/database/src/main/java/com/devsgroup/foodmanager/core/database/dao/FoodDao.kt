package com.devsgroup.foodmanager.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.devsgroup.foodmanager.core.database.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    /** The cached feed, in the server's keyset order. */
    @Query("SELECT * FROM foods WHERE feedPosition IS NOT NULL ORDER BY feedPosition ASC")
    fun observeFeed(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    fun observeFood(id: String): Flow<FoodEntity?>

    /** Liked dishes ("Сохранённое"), most recently updated first. */
    @Query("SELECT * FROM foods WHERE liked = 1 ORDER BY updatedAt DESC")
    fun observeLiked(): Flow<List<FoodEntity>>

    /** Highest position currently assigned, so a newly-fetched page can continue the sequence. */
    @Query("SELECT MAX(feedPosition) FROM foods")
    suspend fun maxFeedPosition(): Int?

    @Upsert
    suspend fun upsertAll(foods: List<FoodEntity>)

    /** Drops a dish from the cached feed (after a swipe) without deleting its cached detail. */
    @Query("UPDATE foods SET feedPosition = NULL WHERE id = :id")
    suspend fun clearFromFeed(id: String)

    @Query("UPDATE foods SET liked = :liked WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean)
}
