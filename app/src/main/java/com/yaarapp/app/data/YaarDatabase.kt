package com.yaarapp.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [User::class, Shop::class, Product::class, CartItem::class, Interest::class, AdCampaign::class],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class YaarDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun shopDao(): ShopDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun interestDao(): InterestDao
    abstract fun adCampaignDao(): AdCampaignDao

    companion object {
        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN firebaseUid TEXT")
                db.execSQL("ALTER TABLE shops ADD COLUMN ownerUid TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN availableCities TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN ownerUid TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN shopRemoteId TEXT")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shops ADD COLUMN certificationPaidAt INTEGER")
                db.execSQL("ALTER TABLE shops ADD COLUMN certificationExpiresAt INTEGER")
            }
        }


        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN email TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shops ADD COLUMN nameChangedAt INTEGER")
                db.execSQL("ALTER TABLE products ADD COLUMN internalDiscussionEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN whatsappDiscussionEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN listingType TEXT NOT NULL DEFAULT 'PRODUCT'")
                db.execSQL("ALTER TABLE products ADD COLUMN secondImageUrl TEXT")
                db.execSQL("ALTER TABLE shops ADD COLUMN logoChangedAt INTEGER")
            }
        }


        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shops ADD COLUMN extraImmoSlots INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * V1.5.3 : correction des notifications "Je suis intéressé" qui se multipliaient.
         * 1) on supprime les doublons déjà présents (on garde la plus ancienne ligne),
         * 2) on ajoute la clé unique remoteId pour empêcher tout nouveau doublon.
         */
        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM interests WHERE id NOT IN (" +
                        "SELECT MIN(id) FROM interests GROUP BY productId, buyerId, shopOwnerId)"
                )
                db.execSQL("ALTER TABLE interests ADD COLUMN remoteId TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_interests_remoteId` ON `interests` (`remoteId`)")
            }
        }

        /** V1.5.4 : certification gratuite 12 mois (date d'activation) + rang de création dans le pays. */
        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shops ADD COLUMN certificationActivatedAt INTEGER")
                db.execSQL("ALTER TABLE shops ADD COLUMN foundingRank INTEGER")
            }
        }

        @Volatile
        private var INSTANCE: YaarDatabase? = null

        fun getInstance(context: Context): YaarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YaarDatabase::class.java,
                    "yaar_app.db"
                )
                    // Version 7 ajoute l'identité Firebase stable du compte, le ciblage
                    // multi-villes des produits et les identifiants Firebase des boutiques.
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
