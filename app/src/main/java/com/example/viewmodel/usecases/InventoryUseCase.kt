package com.example.viewmodel.usecases

import com.example.data.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Result data class for inventory transfers and warehouse operations
 */
data class InventoryTransferResult(
    val isSuccess: Boolean,
    val quantityTransferred: Int,
    val messageTr: String,
    val messageEn: String
)

/**
 * Dedicated UseCase for Inventory management, facility transfers, logistics delivery cost, and storage upgrades.
 */
class InventoryUseCase(
    private val repository: GameRepository
) {
    /**
     * Calculates max storage capacity based on warehouse level and logistics tech
     */
    fun calculateMaxStorageCapacity(
        warehouseLevel: Int,
        logisticsTechLevel: Int
    ): Int {
        val baseCapacity = 1000
        val levelBonus = warehouseLevel * 500
        val techBonus = logisticsTechLevel * 250
        return baseCapacity + levelBonus + techBonus
    }

    /**
     * Calculates warehouse upgrade cost
     */
    fun calculateWarehouseUpgradeCost(currentWarehouseLevel: Int): Long {
        val baseCost = 25000L
        return (baseCost * Math.pow(1.65, currentWarehouseLevel.toDouble())).roundToLong()
    }

    /**
     * Calculates logistics delivery fee and duration based on distance and cargo volume
     */
    fun calculateLogisticsDelivery(
        distanceKm: Float,
        totalWeightTons: Int,
        fleetSpeedTechLevel: Int = 0
    ): Pair<Long, Long> { // (costMoney, durationMs)
        val baseCostPerKmPerTon = 1.25f
        val calculatedCost = (distanceKm * totalWeightTons * baseCostPerKmPerTon).roundToLong().coerceAtLeast(150L)

        val speedMultiplier = (1.0f + fleetSpeedTechLevel * 0.10f).coerceAtLeast(1.0f)
        val baseSeconds = (distanceKm * 0.25f / speedMultiplier).roundToInt().coerceIn(10, 300)
        val durationMs = baseSeconds * 1000L

        return Pair(calculatedCost, durationMs)
    }

    /**
     * Validates transfer of items from central warehouse to a business facility
     */
    fun validateTransferToFacility(
        availableInCentral: Int,
        requestedQty: Int,
        facilityCurrentItems: Int,
        facilityMaxCapacity: Int
    ): InventoryTransferResult {
        if (requestedQty <= 0) {
            return InventoryTransferResult(
                isSuccess = false,
                quantityTransferred = 0,
                messageTr = "Geçersiz transfer miktarı!",
                messageEn = "Invalid transfer quantity!"
            )
        }

        if (availableInCentral < requestedQty) {
            return InventoryTransferResult(
                isSuccess = false,
                quantityTransferred = 0,
                messageTr = "Merkez ambarınızda yeterli ürün bulunmuyor!",
                messageEn = "Insufficient items in central warehouse!"
            )
        }

        val facilityAvailableSpace = (facilityMaxCapacity - facilityCurrentItems).coerceAtLeast(0)
        if (facilityAvailableSpace < requestedQty) {
            return InventoryTransferResult(
                isSuccess = false,
                quantityTransferred = 0,
                messageTr = "Tesis deposunda yeterli boş alan yok! Kalan kapasite: $facilityAvailableSpace ton",
                messageEn = "Insufficient capacity in facility warehouse! Remaining capacity: $facilityAvailableSpace tons"
            )
        }

        return InventoryTransferResult(
            isSuccess = true,
            quantityTransferred = requestedQty,
            messageTr = "📦 $requestedQty ton ürün tesis ambarına aktarıldı.",
            messageEn = "📦 $requestedQty tons transferred to facility warehouse."
        )
    }
}
