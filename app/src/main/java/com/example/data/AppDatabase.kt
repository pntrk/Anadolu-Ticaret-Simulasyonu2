package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlayerEntity::class,
        InventoryEntity::class,
        BusinessEntity::class,
        MarketPriceEntity::class,
        GameStateEntity::class,
        PendingMarketSaleEntity::class,
        MuseumArtifactOwnershipEntity::class,
        MuseumAuctionEntity::class
    ],
    version = 24,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context)
                INSTANCE = instance
                instance
            }
        }

        private fun safeAddColumn(db: SupportSQLiteDatabase, table: String, column: String, typeDef: String) {
            try {
                val cursor = db.query("PRAGMA table_info(`$table`)")
                var exists = false
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex >= 0 && cursor.getString(nameIndex).equals(column, ignoreCase = true)) {
                        exists = true
                        break
                    }
                }
                cursor.close()
                if (!exists) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $typeDef")
                }
            } catch (e: Exception) {
                android.util.Log.w("AppDatabase", "Error ensuring column $column in $table", e)
            }
        }

        private fun migrateToVersion22(db: SupportSQLiteDatabase) {
            try {
                // Ensure players table exists
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `players` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL DEFAULT 'Tüccar',
                        `money` INTEGER NOT NULL DEFAULT 100000,
                        `xp` INTEGER NOT NULL DEFAULT 0,
                        `level` INTEGER NOT NULL DEFAULT 1,
                        `currentCity` TEXT NOT NULL DEFAULT 'istanbul',
                        `isUsdAccount` INTEGER NOT NULL DEFAULT 0,
                        `loanAmount` INTEGER NOT NULL DEFAULT 0,
                        `interestRate` REAL NOT NULL DEFAULT 0.15,
                        `depositBalance` INTEGER NOT NULL DEFAULT 0,
                        `depositInterestRate` REAL NOT NULL DEFAULT 0.05,
                        `dailyIncome` INTEGER NOT NULL DEFAULT 0,
                        `dailyExpense` INTEGER NOT NULL DEFAULT 0,
                        `totalProfit` INTEGER NOT NULL DEFAULT 0,
                        `inventoryCapacity` INTEGER NOT NULL DEFAULT 5000,
                        `isVip` INTEGER NOT NULL DEFAULT 0,
                        `gems` INTEGER NOT NULL DEFAULT 0,
                        `lastDailyRewardMs` INTEGER NOT NULL DEFAULT 0,
                        `loginStreak` INTEGER NOT NULL DEFAULT 0,
                        `lockedDepositBalance` INTEGER NOT NULL DEFAULT 0,
                        `lockedDepositStartTimeMs` INTEGER NOT NULL DEFAULT 0,
                        `lockedDepositDurationMs` INTEGER NOT NULL DEFAULT 0,
                        `dollarBalance` INTEGER NOT NULL DEFAULT 0,
                        `dollarDepositBalance` INTEGER NOT NULL DEFAULT 0,
                        `dollarLoanAmount` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Safely add any missing columns in players table
                safeAddColumn(db, "players", "isUsdAccount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "dollarBalance", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "dollarDepositBalance", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "dollarLoanAmount", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "lockedDepositBalance", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "lockedDepositStartTimeMs", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "lockedDepositDurationMs", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "gems", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "lastDailyRewardMs", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "loginStreak", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "players", "isVip", "INTEGER NOT NULL DEFAULT 0")

                // Inventory table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `inventory` (
                        `itemId` TEXT NOT NULL PRIMARY KEY,
                        `quantity` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Businesses table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `businesses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `cityId` TEXT NOT NULL,
                        `wearLevel` REAL NOT NULL DEFAULT 0.0,
                        `storageCapacity` INTEGER NOT NULL DEFAULT 2500,
                        `storedItemsJson` TEXT NOT NULL DEFAULT '{}',
                        `isUpgrading` INTEGER NOT NULL DEFAULT 0,
                        `upgradeEndTime` INTEGER DEFAULT NULL,
                        `isConstructing` INTEGER NOT NULL DEFAULT 0,
                        `constructionEndTime` INTEGER DEFAULT NULL
                    )
                """.trimIndent())
                safeAddColumn(db, "businesses", "wearLevel", "REAL NOT NULL DEFAULT 0.0")
                safeAddColumn(db, "businesses", "storageCapacity", "INTEGER NOT NULL DEFAULT 2500")
                safeAddColumn(db, "businesses", "storedItemsJson", "TEXT NOT NULL DEFAULT '{}'")
                safeAddColumn(db, "businesses", "isUpgrading", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "businesses", "upgradeEndTime", "INTEGER DEFAULT NULL")
                safeAddColumn(db, "businesses", "isConstructing", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "businesses", "constructionEndTime", "INTEGER DEFAULT NULL")

                // Market prices table with composite primary key (itemId, originCountry)
                // In SQLite, altering primary key requires recreating the table
                db.execSQL("DROP TABLE IF EXISTS `market_prices_temp`")
                db.execSQL("""
                    CREATE TABLE `market_prices_temp` (
                        `itemId` TEXT NOT NULL,
                        `originCountry` TEXT NOT NULL,
                        `originCityId` TEXT NOT NULL,
                        `price` INTEGER NOT NULL,
                        `borsaStock` INTEGER NOT NULL,
                        `isUsd` INTEGER NOT NULL,
                        PRIMARY KEY (`itemId`, `originCountry`)
                    )
                """.trimIndent())
                
                try {
                    val checkCursor = db.query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='market_prices'")
                    val hasTable = checkCursor.moveToFirst() && checkCursor.getInt(0) > 0
                    checkCursor.close()
                    if (hasTable) {
                        db.execSQL("""
                            INSERT OR IGNORE INTO `market_prices_temp` (`itemId`, `originCountry`, `originCityId`, `price`, `borsaStock`, `isUsd`)
                            SELECT 
                                `itemId`, 
                                'Türkiye', 
                                'istanbul', 
                                `price`, 
                                5000, 
                                0 
                            FROM `market_prices`
                        """.trimIndent())
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Non-critical error transferring old market prices", e)
                }

                db.execSQL("DROP TABLE IF EXISTS `market_prices`")
                db.execSQL("ALTER TABLE `market_prices_temp` RENAME TO `market_prices`")

                // Game state table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `game_state` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `season` TEXT NOT NULL DEFAULT 'İlkbahar',
                        `activeEvent` TEXT NOT NULL DEFAULT 'Normal',
                        `globalInflationRate` REAL NOT NULL DEFAULT 0.0,
                        `centralBankLoanRate` REAL NOT NULL DEFAULT 0.15,
                        `centralBankDepositRate` REAL NOT NULL DEFAULT 0.05,
                        `totalMarketLiquidity` INTEGER NOT NULL DEFAULT 10000000,
                        `usdTryRate` REAL NOT NULL DEFAULT 50.0
                    )
                """.trimIndent())
                safeAddColumn(db, "game_state", "usdTryRate", "REAL NOT NULL DEFAULT 50.0")

                // Pending sales table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_sales` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `sellerName` TEXT NOT NULL,
                        `sellerId` TEXT NOT NULL,
                        `itemId` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `pricePerUnit` INTEGER NOT NULL,
                        `originCityId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Museum artifacts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `museum_artifacts` (
                        `artifactId` TEXT NOT NULL PRIMARY KEY,
                        `ownerId` TEXT,
                        `ownerName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `activeAuctionId` TEXT,
                        `lastPrice` INTEGER NOT NULL DEFAULT 0,
                        `updatedAtMs` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Museum auctions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `museum_auctions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `artifactId` TEXT NOT NULL,
                        `sellerId` TEXT NOT NULL,
                        `sellerName` TEXT NOT NULL,
                        `isPlayerSeller` INTEGER NOT NULL,
                        `startingBid` INTEGER NOT NULL,
                        `currentHighestBid` INTEGER NOT NULL,
                        `currentHighestBidderId` TEXT NOT NULL,
                        `currentHighestBidderName` TEXT NOT NULL,
                        `buyoutPrice` INTEGER NOT NULL,
                        `endsAtMs` INTEGER NOT NULL,
                        `bidCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAtMs` INTEGER NOT NULL DEFAULT 0,
                        `isSettled` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error during migration to v22", e)
            }
        }

        fun migrateToVersion23(db: SupportSQLiteDatabase) {
            try {
                migrateToVersion22(db)
                safeAddColumn(db, "businesses", "isUpgrading", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "businesses", "upgradeEndTime", "INTEGER DEFAULT NULL")
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error during migration to v23", e)
            }
        }

        fun migrateToVersion24(db: SupportSQLiteDatabase) {
            try {
                migrateToVersion23(db)
                safeAddColumn(db, "businesses", "isConstructing", "INTEGER NOT NULL DEFAULT 0")
                safeAddColumn(db, "businesses", "constructionEndTime", "INTEGER DEFAULT NULL")
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error during migration to v24", e)
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val directMigrations = (1..23).map { startVersion ->
                object : Migration(startVersion, 24) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        migrateToVersion24(db)
                    }
                }
            }.toTypedArray()

            val stepMigrations = (1..23).map { startVersion ->
                object : Migration(startVersion, startVersion + 1) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        migrateToVersion24(db)
                    }
                }
            }.toTypedArray()

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "anadolu_ticaret_db"
            )
            .addMigrations(*directMigrations, *stepMigrations)
            .fallbackToDestructiveMigration(true)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
        }
    }
}
