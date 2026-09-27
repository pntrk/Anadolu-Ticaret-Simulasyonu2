package com.example.viewmodel.usecases

import com.example.data.*
import kotlin.math.roundToInt

/**
 * Result data class for Mega Project slot contributions
 */
data class MegaProjectContributionResult(
    val isSuccess: Boolean,
    val quantityContributed: Int,
    val remainingNeeded: Int,
    val xpReward: Int,
    val messageTr: String,
    val messageEn: String
)

/**
 * Dedicated UseCase for Mega Project Hub, stages, regional bonuses, and contract milestones.
 * Extracts heavy business calculations from GameViewModel and MegaProjectHubScreen.
 */
class MegaProjectUseCase(
    private val repository: GameRepository
) {
    /**
     * Calculates contribution eligibility and rewards for a Mega Project resource slot
     */
    fun evaluateSlotContribution(
        availableInInventory: Int,
        currentSlotDelivered: Int,
        requiredTotal: Int,
        requestedDeliveryQty: Int,
        productTier: ProductTier = ProductTier.TIER_1,
        supplierFacilityLevel: Int = 1
    ): MegaProjectContributionResult {
        if (availableInInventory <= 0) {
            return MegaProjectContributionResult(
                isSuccess = false,
                quantityContributed = 0,
                remainingNeeded = (requiredTotal - currentSlotDelivered).coerceAtLeast(0),
                xpReward = 0,
                messageTr = "Deponuzda bu ürün bulunmuyor!",
                messageEn = "You don't have this product in your warehouse!"
            )
        }

        val needed = (requiredTotal - currentSlotDelivered).coerceAtLeast(0)
        if (needed <= 0) {
            return MegaProjectContributionResult(
                isSuccess = false,
                quantityContributed = 0,
                remainingNeeded = 0,
                xpReward = 0,
                messageTr = "Bu malzeme ihtiyacı zaten tamamen tamamlanmış!",
                messageEn = "This resource requirement is already fully completed!"
            )
        }

        val toDeliver = requestedDeliveryQty.coerceAtMost(availableInInventory).coerceAtMost(needed)
        if (toDeliver <= 0) {
            return MegaProjectContributionResult(
                isSuccess = false,
                quantityContributed = 0,
                remainingNeeded = needed,
                xpReward = 0,
                messageTr = "Teslim edilecek geçerli miktar girilmedi.",
                messageEn = "No valid delivery quantity specified."
            )
        }

        // XP reward scaled by tier, craftsmanship quality, and quantity
        val baseTierMultiplier = when (productTier) {
            ProductTier.TIER_1 -> 1.0f
            ProductTier.TIER_2 -> 2.5f
            ProductTier.TIER_3 -> 5.0f
            ProductTier.TIER_4 -> 10.0f
        }
        val quality = QualityCraftingService.evaluateConsortiumSlotCraftsmanship(supplierFacilityLevel, productTier)
        val qualityBonusFactor = 1.0f + ((quality.tier - 1) * 0.20f) // 1★: 1.0x, 5★: 1.8x XP
        val xpEarned = (toDeliver * 3 * baseTierMultiplier * qualityBonusFactor).roundToInt().coerceAtLeast(10)
        val remainingAfter = needed - toDeliver
        val qualityTag = if (quality.tier >= 3) " [${quality.starsText} ${quality.labelTr}]" else ""

        return MegaProjectContributionResult(
            isSuccess = true,
            quantityContributed = toDeliver,
            remainingNeeded = remainingAfter,
            xpReward = xpEarned,
            messageTr = "🏗️ $toDeliver ton malzeme başarıyla projeye teslim edildi!$qualityTag (+${xpEarned} XP)",
            messageEn = "🏗️ $toDeliver tons successfully contributed to mega project!$qualityTag (+${xpEarned} XP)"
        )
    }

    /**
     * Calculates the overall project completion percentage across all slots
     */
    fun calculateProjectProgress(
        slots: List<Pair<Int, Int>> // (delivered, required)
    ): Float {
        if (slots.isEmpty()) return 0.0f
        val totalDelivered = slots.sumOf { it.first.toLong() }
        val totalRequired = slots.sumOf { it.second.toLong() }.coerceAtLeast(1L)
        return (totalDelivered.toFloat() / totalRequired.toFloat()).coerceIn(0.0f, 1.0f)
    }

    /**
     * Calculates regional economic bonus multiplier granted by completed mega projects
     */
    fun calculateRegionalBonusMultiplier(
        completedMegaProjectsCount: Int,
        isCityCapital: Boolean
    ): Float {
        val baseBonus = completedMegaProjectsCount * 0.04f // 4% per completed mega project
        val capitalBonus = if (isCityCapital) 0.05f else 0.0f
        return 1.0f + baseBonus + capitalBonus
    }
}
