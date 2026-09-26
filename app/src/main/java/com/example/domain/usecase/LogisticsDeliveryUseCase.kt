package com.example.domain.usecase

import com.example.data.DeliveryItem
import com.example.data.GameRepository
import com.example.data.InventoryEntity
import com.example.data.security.TimeSecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Logistics & Delivery Use Case.
 * Handles background delivery vehicle progression, warehouse arrival, and freight settlement.
 */
class LogisticsDeliveryUseCase(
    private val repository: GameRepository
) {
    data class DeliveryTickResult(
        val remainingDeliveries: List<DeliveryItem>,
        val completedDeliveries: List<DeliveryItem>,
        val totalArrivedValue: Long
    )

    suspend fun processDeliveriesTick(
        activeDeliveries: List<DeliveryItem>
    ): DeliveryTickResult = withContext(Dispatchers.IO) {
        if (activeDeliveries.isEmpty()) {
            return@withContext DeliveryTickResult(emptyList(), emptyList(), 0L)
        }

        val now = TimeSecurityManager.getSecureCurrentTimeMs()
        val completed = mutableListOf<DeliveryItem>()
        val remaining = mutableListOf<DeliveryItem>()
        var totalArrivedValue = 0L

        for (item in activeDeliveries) {
            val elapsed = now - item.startTimeMs
            if (elapsed >= item.totalDurationMs) {
                completed.add(item)
                totalArrivedValue += item.totalCost
                // Deliver items into local player central warehouse inventory
                if (!item.isOutboundSale) {
                    repository.produceItem(item.itemId, item.quantity)
                }
            } else {
                remaining.add(item)
            }
        }

        DeliveryTickResult(
            remainingDeliveries = remaining,
            completedDeliveries = completed,
            totalArrivedValue = totalArrivedValue
        )
    }
}
