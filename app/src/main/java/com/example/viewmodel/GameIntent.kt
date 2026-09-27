package com.example.viewmodel

sealed interface GameIntent {
    // Phase 1 - Dashboard & Core Intents
    object RefreshDashboard : GameIntent
    
    // Phase 2 - Market & Borsa Intents
    data class BuyFromBorsa(val productId: String, val quantity: Int, val originCountry: String = "AUTO") : GameIntent
    data class SellToBorsa(val productId: String, val quantity: Int, val originCountry: String? = null) : GameIntent
    data class CreateMarketListing(val productId: String, val quantity: Int, val price: Long) : GameIntent
    data class CancelMarketListing(val listingId: String) : GameIntent
    data class CreateBuyOrder(val productId: String, val quantity: Int, val maxPrice: Long) : GameIntent
    data class CancelBuyOrder(val orderId: String) : GameIntent
    data class CreateFuturesContract(val productId: String, val quantity: Int, val strikePrice: Long, val durationMinutes: Int, val isCall: Boolean) : GameIntent
    data class CancelFuturesContract(val contractId: String) : GameIntent
    data class FulfillFuturesContract(val contractId: String) : GameIntent
    data class ExerciseFuturesContract(val contractId: String) : GameIntent
    data class SellToBuyOrder(val orderId: String) : GameIntent
    data class UpdateListingPrice(val listingId: String, val newPrice: Long) : GameIntent
    data class AddAuction(val productId: String, val quantity: Int, val startingBid: Long) : GameIntent
    data class BuyFromGlobalMarket(val listingId: String, val quantity: Int) : GameIntent

    // Phase 2 - Inventory & Map Intents
    object UpgradeWarehouseCapacity : GameIntent
    data class BuildBusiness(val facilityId: String, val cityId: String, val cost: Long) : GameIntent
    data class RelocateWarehouse(val targetCityId: String) : GameIntent
    
    // Phase 2 - Bank Intents
    data class SellGems(val gems: Int, val expectedMoney: Long) : GameIntent
    data class TakeLoan(val amount: Long) : GameIntent
    data class RepayLoan(val amount: Long) : GameIntent
    data class DepositMoney(val amount: Long) : GameIntent
    data class WithdrawDeposit(val amount: Long) : GameIntent
    data class SellGuildShares(val guildId: String, val count: Int) : GameIntent
    
    // Phase 2 - Assets & Production Intents
    data class ToggleManagerActive(val managerId: String) : GameIntent
    data class HireManager(val managerId: String) : GameIntent
    data class FireManager(val managerId: String) : GameIntent
    data class UpgradeManager(val managerId: String) : GameIntent
    object ToggleAllManagersActiveStatus : GameIntent
    data class UpgradeBusiness(val business: com.example.data.BusinessEntity) : GameIntent
    data class SpeedUpBusinessUpgradeWithGems(val businessId: Int) : GameIntent
    data class SpeedUpBusinessConstructionWithGems(val businessId: Int) : GameIntent
    data class MaintainBusiness(val business: com.example.data.BusinessEntity) : GameIntent
    data class Produce(val productId: String, val quantity: Int, val autoProcure: Boolean = false) : GameIntent
    object MassHarvestAndProduceAll : GameIntent
    data class BuyMissingIngredients(val productId: String, val quantity: Int) : GameIntent
    data class SellBusiness(val business: com.example.data.BusinessEntity) : GameIntent
    data class ResetManagerDisciplineWithGems(val managerId: String) : GameIntent
    data class SpeedUpResearch(val techKey: String) : GameIntent
    data class SkipProductionWithGems(val productId: String) : GameIntent
    data class SkipDeliveryWithGems(val deliveryId: String) : GameIntent
    data class ApplyTimeWarpWithGems(val hours: Int, val gemCost: Int) : GameIntent
    data class SkipResearchWithGems(val techId: String) : GameIntent

    
    // Facility Warehouse Intents
    data class TransferFacilityStock(val businessId: Int, val itemId: String, val quantity: Int) : GameIntent
    data class TransferAllFacilityStock(val businessId: Int? = null) : GameIntent
    data class SellFacilityStockOnBorsa(val businessId: Int, val itemId: String, val quantity: Int) : GameIntent
    data class SellAllFacilityStockOnBorsa(val businessId: Int? = null) : GameIntent
    
    // Phase 2 - Social & MegaProject Intents
    data class CheckAndClaimMonthlyLeaderboardReward(val forceManualCheck: Boolean) : GameIntent
    data class JoinConsortiumSlot(val projectId: String, val slotId: String, val playerId: String, val playerName: String) : GameIntent
    data class LeaveConsortiumSlot(val projectId: String, val slotId: String) : GameIntent
    data class LeaveEntireConsortium(val projectId: String) : GameIntent
    data class TakeoverBottleneckSlot(val projectId: String, val slotId: String, val playerId: String, val playerName: String) : GameIntent
    data class KickPartnerFromConsortiumSlot(val projectId: String, val slotId: String) : GameIntent
    data class SellConsortiumWarehouseStock(val projectId: String) : GameIntent
    data class AdvanceMegaProjectStage(val projectId: String) : GameIntent
    data class ClaimMegaProjectDividend(val projectId: String) : GameIntent
    data class ProduceConsortiumBrandItem(val projectId: String) : GameIntent
    data class ResetConsortiumNewBatch(val projectId: String) : GameIntent
    data class ListenToConsortiumChat(val projectId: String) : GameIntent
    data class StopListeningToConsortiumChat(val projectId: String) : GameIntent
    data class DisbandConsortium(val projectId: String) : GameIntent
    data class SendConsortiumChatMessage(val projectId: String, val text: String) : GameIntent
    data class CreateNewMegaProject(
        val consortiumName: String,
        val brandName: String,
        val targetProductId: String,
        val qualityTier: com.example.data.ConsortiumQualityTier = com.example.data.ConsortiumQualityTier.GRADE_C,
        val founderClaimedProductIds: Set<String> = emptySet(),
        val cityId: String
    ) : GameIntent
    data class DeliverMaterialsToConsortium(val projectId: String, val slotId: String, val quantity: Int) : GameIntent
    data class ApproveConsortiumMassProduction(val projectId: String) : GameIntent
    data class ToggleConsortiumProductionState(val projectId: String) : GameIntent
    data class ToggleConsortiumAutoSell(val projectId: String, val autoSellActive: Boolean) : GameIntent
    data class ChangeConsortiumSalesChannel(val projectId: String, val channel: com.example.data.ConsortiumSalesChannel) : GameIntent
    data class AssignConsortiumRole(val projectId: String, val targetPlayerId: String, val targetPlayerName: String, val role: com.example.data.ConsortiumRole) : GameIntent
    data class ElectConsortiumRoles(val projectId: String) : GameIntent
    data class NudgeConsortiumPartner(val projectId: String, val slotId: String) : GameIntent
    data class BroadcastConsortiumRadioSos(val projectId: String, val slotId: String) : GameIntent
    data class OneTapDeliverToConsortium(val projectId: String, val slotId: String) : GameIntent
    data class FulfillConsortiumExportTender(val projectId: String, val tenderId: String, val quantity: Int) : GameIntent
    data class SetConsortiumProductionStrategy(val projectId: String, val strategy: com.example.data.ConsortiumProductionStrategy) : GameIntent
    data class CreateConsortiumBoardProposal(
        val projectId: String,
        val titleTr: String,
        val titleEn: String,
        val descriptionTr: String,
        val descriptionEn: String,
        val proposalType: com.example.data.ConsortiumProposalType,
        val proposedValue: String
    ) : GameIntent
    data class VoteOnConsortiumBoardProposal(val projectId: String, val proposalId: String, val voteYes: Boolean) : GameIntent
    
    // Phase 2 - Auth Intents
    data class UpdateCompanyName(val newName: String) : GameIntent
    data class SignInAnonymously(val onResult: (Boolean, String?) -> Unit) : GameIntent
    
    // Manager Intents
    // data class HireManager(...) : GameIntent
    
    // Consortium Intents
    // data class DeliverMaterial(...) : GameIntent
}