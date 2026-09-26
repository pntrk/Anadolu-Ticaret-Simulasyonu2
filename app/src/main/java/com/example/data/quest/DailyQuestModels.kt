package com.example.data.quest

import kotlinx.serialization.Serializable

/**
 * Quest Action Types for Tracking and Progress Evaluation.
 */
@Serializable
enum class QuestType {
    SELL_COMMODITY,
    BORSA_TRADE,
    FACILITY_PRODUCE,
    LOGISTICS_DELIVERY,
    BANK_DEPOSIT,
    MUSEUM_BID,
    RD_INVEST,
    DAILY_LOGIN
}

/**
 * Individual Daily Quest Model.
 */
@Serializable
data class DailyQuest(
    val id: String,
    val title: String,
    val titleEn: String,
    val description: String,
    val descriptionEn: String,
    val iconName: String,
    val targetAmount: Long,
    val currentProgress: Long = 0L,
    val rewardMoney: Long = 250_000L,
    val rewardGems: Int = 5,
    val rewardSeasonXp: Int = 100,
    val isClaimed: Boolean = false,
    val type: QuestType,
    val targetRoute: String = "home"
) {
    val isCompleted: Boolean
        get() = currentProgress >= targetAmount

    val progressFraction: Float
        get() = if (targetAmount <= 0L) 1.0f else (currentProgress.toFloat() / targetAmount.toFloat()).coerceIn(0.0f, 1.0f)
}

/**
 * Season Pass Milestone Tier.
 */
@Serializable
data class SeasonPassTier(
    val tierLevel: Int,
    val requiredXp: Int,
    val freeRewardName: String,
    val freeRewardNameEn: String,
    val freeRewardMoney: Long,
    val freeRewardGems: Int,
    val vipRewardName: String,
    val vipRewardNameEn: String,
    val isFreeClaimed: Boolean = false,
    val isVipClaimed: Boolean = false
)

/**
 * Consolidated State of Daily Quests and Season Battle Pass.
 */
@Serializable
data class DailyQuestState(
    val quests: List<DailyQuest> = emptyList(),
    val seasonXp: Int = 0,
    val seasonLevel: Int = 1,
    val lastResetDateKey: String = "",
    val isVipPassUnlocked: Boolean = false,
    val claimedFreeTiers: Set<Int> = emptySet(),
    val claimedVipTiers: Set<Int> = emptySet(),
    val claimedQuestIdsToday: Set<String> = emptySet()
)
