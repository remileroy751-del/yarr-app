package com.yaarapp.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [User::class, Shop::class, Product::class, CartItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class YaarDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun shopDao(): ShopDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao

    companion object {
        @Volatile
        private var INSTANCE: YaarDatabase? = null

        fun getInstance(context: Context): YaarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YaarDatabase::class.java,
                    "yaar_app.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
