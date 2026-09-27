package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.random.Random

/**
 * İcralık tesis ihalesi veri modeli.
 */
@Serializable
data class ForeclosureAuction(
    val id: String,
    val originalOwnerId: String,
    val originalOwnerName: String,
    val facilityType: String,
    val cityId: String,
    val level: Int,
    val startingBid: Long,
    val buyoutPrice: Long,
    val currentHighestBid: Long,
    val highestBidderId: String? = null,
    val highestBidderName: String? = null,
    val reason: String,
    val endsAtMs: Long,
    val isSettled: Boolean = false
)

/**
 * İcralık tesislerin ve açık artırma süreçlerinin yönetildiği singleton nesne.
 */
object ForeclosureManager {

    private val _auctions = MutableStateFlow<List<ForeclosureAuction>>(emptyList())
    val auctions: StateFlow<List<ForeclosureAuction>> = _auctions.asStateFlow()
    val auctionsState: StateFlow<List<ForeclosureAuction>> = _auctions.asStateFlow()

    fun addAuction(auction: ForeclosureAuction) {
        // Tier 4 tesisler (Mega Projeler) haczedilemez ve iflas masasında satışa sunulamaz
        val prod = Product.values().find { it.facilityId == auction.facilityType || it.id == auction.facilityType }
        if (prod?.tier == ProductTier.TIER_4) return
        _auctions.value = _auctions.value + auction
    }

    private val foreclosureReasons = listOf(
        "Banka Kredi Temerrüdü ve Haciz Kararı",
        "Vergi Borçları Nedeniyle Hazine Satışı",
        "İflas Masası Tasfiye Açık Artırması",
        "Tedarik Zinciri Kırılması ve Borç Tasfiyesi",
        "Şirket Tasfiyesi ve Mahkeme Kararıyla Satış",
        "Enerji ve Lojistik Borçları Nedeniyle İcra Takibi"
    )

    init {
        // İlk açılışta ihale havuzunu doldur
        generateBotAuctions()
    }

    /**
     * Aktif (süresi dolmamış ve sonuçlanmamış) ihaleleri döner.
     */
    fun getActiveAuctions(): List<ForeclosureAuction> {
        val now = System.currentTimeMillis()
        return _auctions.value.filter { !it.isSettled && it.endsAtMs > now }
    }

    /**
     * Eğer aktif ihale sayısı 3'ten azsa, Product listesinden ve BotTycoonManager'daki
     * botlardan rastgele 2-3 adet icralık tesis oluşturur (Tier 4 hariç).
     * buyoutPrice = Product.facilityCost * level * 0.45
     */
    fun generateBotAuctions() {
        val now = System.currentTimeMillis()
        // Süresi bitmiş ihaleleri de temizle veya settle et
        val currentActive = _auctions.value.filter { !it.isSettled && it.endsAtMs > now }
        if (currentActive.size >= 3) return

        val bots = BotTycoonManager.getAllBots()
        // KURAL: Tier 4 tesisler kesinlikle iflas masasına giremez
        val availableProducts = Product.values().filter { it.facilityCost > 0L && it.tier != ProductTier.TIER_4 }
        if (availableProducts.isEmpty()) return

        val countToGenerate = Random.nextInt(2, 4) // 2 veya 3 adet
        val newAuctions = mutableListOf<ForeclosureAuction>()

        for (i in 0 until countToGenerate) {
            val product = availableProducts.random()
            val bot = if (bots.isNotEmpty()) bots.random() else null

            val ownerId = bot?.id ?: "BOT-${Random.nextInt(100, 999)}"
            val ownerName = bot?.companyName ?: "Tasfiye Halinde Şirket A.Ş."
            val city = bot?.cityId ?: "istanbul"
            val facilityLvl = Random.nextInt(1, 4) // 1 ile 3 seviye arası

            // buyoutPrice = Product.facilityCost * level * 0.45
            val calculatedBuyout = (product.facilityCost * facilityLvl * 0.45).toLong().coerceAtLeast(20_000L)
            val startingBid = (calculatedBuyout * 0.40).toLong().coerceAtLeast(10_000L)
            val durationMinutes = Random.nextLong(15, 61) // 15 - 60 dakika

            val auction = ForeclosureAuction(
                id = "AUC-${UUID.randomUUID().toString().take(8).uppercase()}",
                originalOwnerId = ownerId,
                originalOwnerName = ownerName,
                facilityType = product.facilityId,
                cityId = city,
                level = facilityLvl,
                startingBid = startingBid,
                buyoutPrice = calculatedBuyout,
                currentHighestBid = startingBid,
                highestBidderId = null,
                highestBidderName = null,
                reason = foreclosureReasons.random(),
                endsAtMs = now + (durationMinutes * 60 * 1000L),
                isSettled = false
            )
            newAuctions.add(auction)
        }

        _auctions.value = currentActive + newAuctions
    }

    /**
     * Belirtilen ihaleye oyuncu tarafından pey / teklif verilmesini sağlar.
     */
    fun placeBid(auctionId: String, player: PlayerEntity, bidAmount: Long): Boolean {
        val now = System.currentTimeMillis()
        val currentList = _auctions.value
        val target = currentList.find { it.id == auctionId } ?: return false

        // İhale kapandıysa veya süresi dolduysa teklif verilemez
        if (target.isSettled || target.endsAtMs <= now) return false

        // Teklif mevcut en yüksek tekliften ve başlangıç teklifinden büyük olmalı
        if (bidAmount <= target.currentHighestBid || bidAmount < target.startingBid) return false

        // Oyuncunun yeterli nakdi olmalı
        if (player.money < bidAmount) return false

        // Hemen al fiyatından büyükse buyoutFacility kullanılmalı veya buyout olarak işlenmeli
        val finalBid = bidAmount.coerceAtMost(target.buyoutPrice)
        val isFullBuyout = finalBid >= target.buyoutPrice

        _auctions.value = currentList.map { auction ->
            if (auction.id == auctionId) {
                auction.copy(
                    currentHighestBid = finalBid,
                    highestBidderId = player.id,
                    highestBidderName = player.name,
                    isSettled = isFullBuyout
                )
            } else {
                auction
            }
        }
        return true
    }

    /**
     * Süresi dolan ihaleleri kontrol eder. Eğer oyuncu en yüksek teklifi vermişse
     * tesisi oyuncuya kazandırır ve tesis listesi ile kesilecek tutarı döner.
     */
    data class AuctionWinResult(
        val wonBusiness: BusinessEntity,
        val winningBidAmount: Long,
        val auctionId: String,
        val facilityName: String
    )

    fun settleCompletedAuctions(player: PlayerEntity): List<AuctionWinResult> {
        val now = System.currentTimeMillis()
        val currentList = _auctions.value
        val wins = mutableListOf<AuctionWinResult>()
        val updatedList = mutableListOf<ForeclosureAuction>()

        for (auction in currentList) {
            if (!auction.isSettled && auction.endsAtMs <= now) {
                if (auction.highestBidderId == player.id) {
                    val business = BusinessEntity(
                        id = 0,
                        type = auction.facilityType,
                        level = auction.level,
                        cityId = auction.cityId,
                        wearLevel = 0.0f,
                        storageCapacity = auction.level.coerceIn(1, 10) * 500,
                        storedItemsJson = "{}",
                        isUpgrading = false,
                        upgradeEndTime = null,
                        isConstructing = false,
                        constructionEndTime = null
                    )
                    wins.add(
                        AuctionWinResult(
                            wonBusiness = business,
                            winningBidAmount = auction.currentHighestBid,
                            auctionId = auction.id,
                            facilityName = auction.facilityType
                        )
                    )
                }
                updatedList.add(auction.copy(isSettled = true))
            } else {
                updatedList.add(auction)
            }
        }

        _auctions.value = updatedList
        generateBotAuctions()
        return wins
    }

    /**
     * Tesisin 'Hemen Al' bedeli üzerinden doğrudan satın alınması.
     * Satın alma başarılı olursa aşınması sıfır (wearLevel = 0.0f) olan hazır BusinessEntity döndürür.
     */
    fun buyoutFacility(auctionId: String, player: PlayerEntity): BusinessEntity? {
        val now = System.currentTimeMillis()
        val currentList = _auctions.value
        val target = currentList.find { it.id == auctionId } ?: return null

        if (target.isSettled || target.endsAtMs <= now) return null
        if (player.money < target.buyoutPrice) return null

        // İhaleyi kapat ve kazanan olarak oyuncuyu ata
        _auctions.value = currentList.map { auction ->
            if (auction.id == auctionId) {
                auction.copy(
                    currentHighestBid = target.buyoutPrice,
                    highestBidderId = player.id,
                    highestBidderName = player.name,
                    isSettled = true
                )
            } else {
                auction
            }
        }

        // Aşınması sıfır (wearLevel = 0.0f) olan yeni tesis üret ve döndür
        return BusinessEntity(
            id = 0,
            type = target.facilityType,
            level = target.level,
            cityId = target.cityId,
            wearLevel = 0.0f,
            storageCapacity = target.level.coerceIn(1, 10) * 500,
            storedItemsJson = "{}",
            isUpgrading = false,
            upgradeEndTime = null,
            isConstructing = false,
            constructionEndTime = null
        )
    }

    /**
     * Süresi dolmamış ihalelerde botların rastgele %15 ihtimalle teklif artırmasını sağlar.
     */
    fun processBotBiddingCycle() {
        val now = System.currentTimeMillis()
        val bots = BotTycoonManager.getAllBots()
        if (bots.isEmpty()) return

        val currentList = _auctions.value
        var changed = false

        val updatedList = currentList.map { auction ->
            if (!auction.isSettled && auction.endsAtMs > now) {
                // Rastgele %15 ihtimalle teklif artırma
                if (Random.nextFloat() < 0.15f) {
                    val competingBot = bots.filter { it.id != auction.highestBidderId }.randomOrNull()
                    if (competingBot != null) {
                        // Teklif artışı: Buyout fiyatının %3 ile %7'si arasında
                        val increment = (auction.buyoutPrice * Random.nextDouble(0.03, 0.08)).toLong().coerceAtLeast(2_500L)
                        val newBid = auction.currentHighestBid + increment

                        // Hemen Al fiyatını aşmayacak şekilde sınırla
                        if (newBid < auction.buyoutPrice) {
                            changed = true
                            auction.copy(
                                currentHighestBid = newBid,
                                highestBidderId = competingBot.id,
                                highestBidderName = competingBot.companyName
                            )
                        } else {
                            auction
                        }
                    } else {
                        auction
                    }
                } else {
                    auction
                }
            } else {
                auction
            }
        }

        if (changed) {
            _auctions.value = updatedList
        }
    }
}
