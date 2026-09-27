package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM players LIMIT 1")
    fun getPlayer(): Flow<PlayerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)

    @Query("SELECT * FROM inventory")
    fun getInventory(): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: InventoryEntity)

    @Query("SELECT * FROM businesses")
    fun getBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses")
    suspend fun getBusinessesDirect(): List<BusinessEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBusinesses(businesses: List<BusinessEntity>)

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusinessById(id: Int)

    @Query("DELETE FROM businesses")
    suspend fun deleteAllBusinesses()

    @Query("SELECT * FROM market_prices")
    fun getMarketPrices(): Flow<List<MarketPriceEntity>>

    @Query("SELECT * FROM market_prices")
    suspend fun getMarketPricesDirect(): List<MarketPriceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketPrices(prices: List<MarketPriceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInventory(inventoryList: List<InventoryEntity>)

    @Query("SELECT * FROM game_state LIMIT 1")
    fun getGameState(): Flow<GameStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameState(state: GameStateEntity)

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()

    @Query("UPDATE players SET money = money + :amount")
    suspend fun addMoney(amount: Long)

    @Query("UPDATE players SET xp = xp + :amount")
    suspend fun addXp(amount: Int)

    @Query("SELECT * FROM inventory WHERE itemId = :itemId")
    suspend fun getInventoryItem(itemId: String): InventoryEntity?

    @Query("UPDATE inventory SET quantity = quantity + :amount WHERE itemId = :itemId")
    suspend fun _updateInventoryQuantity(itemId: String, amount: Int)

    @Query("DELETE FROM inventory WHERE quantity <= 0")
    suspend fun cleanUpEmptyInventory()

    @Transaction
    suspend fun updateInventoryQuantity(itemId: String, amount: Int) {
        _updateInventoryQuantity(itemId, amount)
        cleanUpEmptyInventory()
    }

    /**
     * Atomically processes item sales (reduces inventory, adds revenue, adds XP) in a single SQLite WAL transaction.
     */
    @Transaction
    suspend fun sellItemTransaction(itemId: String, quantity: Int, totalRevenue: Long, xpAmount: Int) {
        _updateInventoryQuantity(itemId, -quantity)
        cleanUpEmptyInventory()
        addMoney(totalRevenue)
        addXp(xpAmount)
    }

    /**
     * Atomically adds or updates item quantity and awards XP in a single SQLite WAL transaction.
     */
    @Transaction
    suspend fun addOrUpdateInventoryTransaction(itemId: String, quantity: Int, xpAmount: Int = 0) {
        val existing = getInventoryItem(itemId)
        if (existing != null) {
            _updateInventoryQuantity(itemId, quantity)
        } else {
            insertInventory(InventoryEntity(itemId = itemId, quantity = quantity))
        }
        cleanUpEmptyInventory()
        if (xpAmount > 0) {
            addXp(xpAmount)
        }
    }

    /**
     * Atomically deducts money, builds new business, and awards XP in a single SQLite WAL transaction.
     */
    @Transaction
    suspend fun buyBusinessTransaction(cost: Long, business: BusinessEntity, xpAmount: Int = 50) {
        addMoney(-cost)
        insertBusiness(business)
        if (xpAmount > 0) {
            addXp(xpAmount)
        }
    }

    /**
     * Atomically clears existing businesses and inserts new ones in a single SQLite WAL transaction.
     */
    @Transaction
    suspend fun clearAndReplaceBusinessesTransaction(newList: List<BusinessEntity>) {
        deleteAllBusinesses()
        insertAllBusinesses(newList)
    }

    /**
     * Atomically processes trade transactions (deducts logistics cost, adds revenue, awards XP).
     */
    @Transaction
    suspend fun processTradeTransaction(logisticsCost: Long, revenue: Long, xpAmount: Int = 25) {
        addMoney(revenue - logisticsCost)
        addXp(xpAmount)
    }

    /**
     * Atomically claims pending market sale proceeds.
     */
    @Transaction
    suspend fun claimPendingSaleTransaction(saleId: String, netRevenue: Long, xpAmount: Int = 25) {
        addMoney(netRevenue)
        addXp(xpAmount)
        deletePendingSale(saleId)
    }

    /**
     * Atomically restores full player state and entities in a single SQLite WAL transaction to prevent partial data corruption.
     */
    @Transaction
    suspend fun restoreFullPlayerStateTransaction(
        player: PlayerEntity,
        state: GameStateEntity,
        businesses: List<BusinessEntity>,
        inventory: List<InventoryEntity>,
        prices: List<MarketPriceEntity>
    ) {
        deleteAllPlayers()
        insertPlayer(player)
        insertGameState(state)
        deleteAllBusinesses()
        if (businesses.isNotEmpty()) {
            insertAllBusinesses(businesses)
        }
        if (inventory.isNotEmpty()) {
            insertAllInventory(inventory)
        }
        if (prices.isNotEmpty()) {
            insertMarketPrices(prices)
        }
    }

    @Query("SELECT * FROM pending_sales")
    suspend fun getPendingSales(): List<PendingMarketSaleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSale(sale: PendingMarketSaleEntity)

    @Query("DELETE FROM pending_sales WHERE id = :id")
    suspend fun deletePendingSale(id: String)

    @Query("DELETE FROM pending_sales")
    suspend fun deleteAllPendingSales()

    // =========================================================================
    // 1-OF-1 MUSEUM ARTIFACTS REGISTRY & REAL PLAYER AUCTIONS
    // =========================================================================

    @Query("SELECT * FROM museum_artifacts")
    fun getAllMuseumArtifactsFlow(): Flow<List<MuseumArtifactOwnershipEntity>>

    @Query("SELECT * FROM museum_artifacts")
    suspend fun getAllMuseumArtifacts(): List<MuseumArtifactOwnershipEntity>

    @Query("SELECT * FROM museum_artifacts WHERE artifactId = :artifactId LIMIT 1")
    suspend fun getMuseumArtifact(artifactId: String): MuseumArtifactOwnershipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMuseumArtifact(artifact: MuseumArtifactOwnershipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMuseumArtifacts(artifacts: List<MuseumArtifactOwnershipEntity>)

    @Query("DELETE FROM museum_artifacts")
    suspend fun deleteAllMuseumArtifacts()

    @Query("SELECT * FROM museum_auctions WHERE isSettled = 0")
    fun getActiveMuseumAuctionsFlow(): Flow<List<MuseumAuctionEntity>>

    @Query("SELECT * FROM museum_auctions")
    suspend fun getAllMuseumAuctions(): List<MuseumAuctionEntity>

    @Query("SELECT * FROM museum_auctions WHERE id = :auctionId LIMIT 1")
    suspend fun getMuseumAuctionById(auctionId: String): MuseumAuctionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuseumAuction(auction: MuseumAuctionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMuseumAuctions(auctions: List<MuseumAuctionEntity>)

    @Query("DELETE FROM museum_auctions WHERE id = :auctionId")
    suspend fun deleteMuseumAuction(auctionId: String)

    @Query("DELETE FROM museum_auctions")
    suspend fun deleteAllMuseumAuctions()
}
