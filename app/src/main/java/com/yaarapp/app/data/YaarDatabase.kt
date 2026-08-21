package com.yaarapp.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [User::class, Shop::class, Product::class, CartItem::class],
    version = 2,
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
                )
                    // Version 2 introduit pays/ville/sexe et le système d'expiration des
                    // produits. Comme il s'agit d'une base locale de démonstration (pas de
                    // données critiques côté serveur), on repart d'une base propre au lieu
                    // d'écrire une migration détaillée. À remplacer par une vraie migration
                    // Room (ou par la bascule vers Firestore, voir /BACKEND_FIREBASE.md)
                    // avant toute mise en production avec de vraies données utilisateurs.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
