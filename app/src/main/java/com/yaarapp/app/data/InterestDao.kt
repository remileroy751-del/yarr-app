package com.yaarapp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestDao {
    @Query("SELECT * FROM interests WHERE shopOwnerId = :ownerId ORDER BY createdAt DESC")
    fun observeForOwner(ownerId: Int): Flow<List<Interest>>

    @Query("SELECT COUNT(*) FROM interests WHERE shopOwnerId = :ownerId AND isRead = 0")
    fun observeUnreadCount(ownerId: Int): Flow<Int>

    @Query("SELECT * FROM interests WHERE productId = :productId AND buyerId = :buyerId AND ABS(createdAt - :createdAt) < 10000 LIMIT 1")
    suspend fun findLatest(productId: Int, buyerId: Int, createdAt: Long): Interest?

    @Query("SELECT * FROM interests WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): Interest?

    /** Ancienne ligne locale pas encore rattachée à un identifiant Supabase (avant V1.5.3). */
    @Query("SELECT * FROM interests WHERE remoteId IS NULL AND productId = :productId AND buyerId = :buyerId AND shopOwnerId = :ownerId ORDER BY id ASC LIMIT 1")
    suspend fun findUnlinked(productId: Int, buyerId: Int, ownerId: Int): Interest?

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insert(interest: Interest): Long

    @Update
    suspend fun update(interest: Interest)

    @Query("DELETE FROM interests WHERE shopOwnerId = :ownerId OR buyerId = :ownerId")
    suspend fun deleteAllForUser(ownerId: Int)
}
