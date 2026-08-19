package com.yaarapp.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): User?

    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}
