package com.example.domain.manager

import com.example.data.MuseumAuctionEntity
import com.example.data.PlayerEntity
import com.example.data.SupabaseManager
import com.example.data.security.TimeSecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Museum & Global Rare Artifact Auction Domain Manager.
 * Handles online Supabase bidding, buyout instant acquisitions, and offline settlement.
 */
class AuctionDomainManager {

    sealed class BidResult {
        data class Success(val updatedAuction: MuseumAuctionEntity, val deductedMoney: Long) : BidResult()
        data class Outbid(val currentHighestBid: Long) : BidResult()
        data class InsufficientFunds(val requiredBid: Long, val available: Long) : BidResult()
        data class Expired(val message: String) : BidResult()
        data class Error(val message: String) : BidResult()
    }

    suspend fun placeBid(
        auction: MuseumAuctionEntity,
        bidAmount: Long,
        player: PlayerEntity,
        isOnline: Boolean
    ): BidResult = withContext(Dispatchers.IO) {
        val now = TimeSecurityManager.getSecureCurrentTimeMs()
        if (auction.endsAtMs > 0L && now >= auction.endsAtMs) {
            return@withContext BidResult.Expired("Bu müzayedenin süresi doldu!")
        }

        val minRequiredBid = if (auction.currentHighestBid > 0L) {
            (auction.currentHighestBid * 1.05).toLong() // 5% minimum bid increment
        } else {
            auction.startingBid
        }

        if (bidAmount < minRequiredBid) {
            return@withContext BidResult.Outbid(minRequiredBid)
        }

        if (player.money < bidAmount) {
            return@withContext BidResult.InsufficientFunds(requiredBid = bidAmount, available = player.money)
        }

        val updatedAuction = auction.copy(
            currentHighestBid = bidAmount,
            currentHighestBidderId = player.id,
            currentHighestBidderName = player.name,
            bidCount = auction.bidCount + 1
        )

        if (isOnline) {
            val remoteSuccess = SupabaseManager.updateMuseumAuctionBidInSupabase(
                auctionId = auction.id,
                newBid = bidAmount,
                bidderId = player.id,
                bidderName = player.name
            )
            if (!remoteSuccess) {
                return@withContext BidResult.Error("Sunucu ile teklif senkronize edilemedi.")
            }
        }

        BidResult.Success(updatedAuction = updatedAuction, deductedMoney = bidAmount)
    }
}
