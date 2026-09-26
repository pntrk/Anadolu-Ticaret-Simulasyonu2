package com.example.viewmodel

import com.example.data.GameRepository
import com.example.data.PendingMarketSaleEntity
import com.example.data.MarketListing
import android.util.Log

class MarketManager(private val repository: GameRepository) {

    suspend fun syncPendingSales() {
        try {
            val pending = repository.getPendingSales()
            if (pending.isEmpty()) return

            for (sale in pending) {
                val listing = MarketListing(
                    id = sale.id,
                    sellerName = sale.sellerName,
                    sellerId = sale.sellerId,
                    itemId = sale.itemId,
                    quantity = sale.quantity,
                    pricePerUnit = sale.pricePerUnit,
                    originCityId = sale.originCityId,
                    createdAt = sale.createdAt
                )
                
                try {
                    repository.deletePendingSale(sale.id)
                } catch (e: Exception) {
                    Log.e("MarketManager", "Error syncing sale", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MarketManager", "Error in sync loop", e)
        }
    }

    suspend fun createMarketListing(
        sellerName: String,
        itemId: String,
        quantity: Int,
        pricePerUnit: Long,
        originCityId: String
    ) {
        val uid = "trader_${sellerName.hashCode()}"
        val sale = PendingMarketSaleEntity(
            sellerName = sellerName,
            sellerId = uid,
            itemId = itemId,
            quantity = quantity,
            pricePerUnit = pricePerUnit,
            originCityId = originCityId
        )
        
        repository.insertPendingSale(sale)
        
        // consume the items from inventory immediately
        repository.consumeItem(itemId, quantity)
        
        // Try syncing immediately
        syncPendingSales()
    }
}
