package com.yaarapp.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    /** Flux marketplace : tous les produits de toutes les boutiques, les plus récents en premier. */
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT DISTINCT category FROM products ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun observeByShop(shopId: Int): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Int): Product?

    @Query("SELECT COUNT(*) FROM products WHERE shopId = :shopId")
    suspend fun countForShop(shopId: Int): Int

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Insert
    suspend fun insertAll(products: List<Product>)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}
