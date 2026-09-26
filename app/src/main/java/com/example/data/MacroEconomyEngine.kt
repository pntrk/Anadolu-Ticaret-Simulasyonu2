package com.example.data

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.tanh
import kotlin.random.Random

enum class EconomicCycle {
    BOOM,       // Genişleme (Yüksek büyüme, artan enflasyon)
    PEAK,       // Zirve (Yüksek enflasyon, faiz artışları başlar)
    RECESSION,  // Daralma (Düşen talep, azalan enflasyon, borsa düşüşü)
    DEPRESSION, // Dip (Düşük faiz, düşük fiyatlar, borsada alım fırsatı)
    RECOVERY    // Toparlanma (Artan talep, borsada yükseliş)
}

data class MacroEconomyState(
    val cycle: EconomicCycle = EconomicCycle.RECOVERY,
    val globalInflationRate: Float = 0.05f,
    val centralBankInterestRate: Float = 0.10f, // Mevduat ve kredi için baz faiz
    val marketLiquidityMultiplier: Float = 1.0f, // Borsaya giren paranın hacmi
    val peRatioBase: Float = 10.0f // F/K Çarpanı (Hisse fiyatlaması için)
)

object MacroEconomyEngine {

    fun simulateNextPhase(currentState: MacroEconomyState): MacroEconomyState {
        var nextCycle = currentState.cycle
        var inflation = currentState.globalInflationRate
        var interest = currentState.centralBankInterestRate
        var liquidity = currentState.marketLiquidityMultiplier
        var peBase = currentState.peRatioBase

        // 1. Döngü Geçiş İhtimalleri (Her tick'te %15 ihtimalle döngü ilerleyebilir)
        if (Random.nextFloat() < 0.15f) {
            nextCycle = when (currentState.cycle) {
                EconomicCycle.RECOVERY -> EconomicCycle.BOOM
                EconomicCycle.BOOM -> EconomicCycle.PEAK
                EconomicCycle.PEAK -> EconomicCycle.RECESSION
                EconomicCycle.RECESSION -> EconomicCycle.DEPRESSION
                EconomicCycle.DEPRESSION -> EconomicCycle.RECOVERY
            }
        }

        // 2. Döngüye Göre Enflasyon ve Talep Değişimi (Sistem Dinamikleri Doygunluğu)
        when (nextCycle) {
            EconomicCycle.RECOVERY -> {
                inflation += Random.nextFloat() * 0.01f
                liquidity = (liquidity + 0.05f).coerceAtMost(1.5f)
                peBase = (peBase + 0.5f).coerceAtMost(15f)
            }
            EconomicCycle.BOOM -> {
                inflation += Random.nextFloat() * 0.02f
                liquidity = (liquidity + 0.1f).coerceAtMost(2.0f)
                peBase = (peBase + 1.0f).coerceAtMost(25f) // Borsa ralli yapar
            }
            EconomicCycle.PEAK -> {
                inflation += Random.nextFloat() * 0.03f
                liquidity = (liquidity - 0.1f).coerceAtLeast(1.0f) // Akıllı para çıkmaya başlar
                peBase = (peBase - 1.0f).coerceAtLeast(12f)
            }
            EconomicCycle.RECESSION -> {
                inflation -= Random.nextFloat() * 0.02f
                liquidity = (liquidity - 0.2f).coerceAtLeast(0.5f)
                peBase = (peBase - 2.0f).coerceAtLeast(5f) // Borsa çakılır
            }
            EconomicCycle.DEPRESSION -> {
                inflation -= Random.nextFloat() * 0.01f
                liquidity = (liquidity + 0.02f).coerceAtLeast(0.3f)
                peBase = (peBase + 0.2f).coerceAtMost(8f)
            }
        }

        // Sınırlandırmalar
        inflation = inflation.coerceIn(-0.02f, 0.50f) // -%2 ile %50 arası enflasyon

        // 3. Merkez Bankası Reaksiyonu (Taylor Kuralı Benzeri)
        val targetInflation = 0.05f
        if (inflation > targetInflation + 0.02f) {
            interest += 0.02f // Şahin tepki
        } else if (inflation < targetInflation - 0.01f) {
            interest -= 0.01f // Güvercin tepki
        }
        interest = interest.coerceIn(0.01f, 0.40f) // %1 ile %40 arası faiz

        // Faiz artarsa borsa (P/E) düşer, likidite bankaya kaçar
        peBase -= (interest * 10f)
        peBase = peBase.coerceIn(3f, 30f)

        return MacroEconomyState(
            cycle = nextCycle,
            globalInflationRate = inflation,
            centralBankInterestRate = interest,
            marketLiquidityMultiplier = liquidity,
            peRatioBase = peBase
        )
    }

    /**
     * Şirket hisse değerlemesi hesaplama (rawPrice).
     *
     * Milyonlarca oyunculu devasa ekonomiler için Sistem Dinamikleri (System Dynamics) prensipleri:
     * 1. Cobb-Douglas / Log-Scale Azalan Getiri (Diminishing Returns on Valuation):
     *    Devasa trilyonluk şirket değerlemelerinde hisse fiyatının sonsuza ıraksaması önlenir.
     * 2. XP & Seviye İlerlemesinde Logaritmik Azalan Getiri (Diminishing Returns):
     *    Lineer çarpan yerine logaritmik azalan getiri eğrisi ile seviye arttıkça kazanılan
     *    borsa çarpanının etkisi zamanla yavaşlar, ileri seviye oyuncuların hiperenflasyon
     *    yaratması matematiksel olarak engellenir.
     * 3. Kocaeli Sanayi Grevi (CityMarketEvent) ve Dışsal Şok Sönümleyicisi:
     *    Sisteme aniden giren bölgesel kriz ve grev şoklarının yaratacağı enflasyonist etki,
     *    hiperbolik tanjant (tanh) elastik sönümleme filtresi ile dengelenir.
     */
    fun calculateCompanySharePrice(
        totalValuation: Long,
        xpProgress: Int,
        macroState: MacroEconomyState,
        crisisState: ConsortiumCrisisState? = null,
        activeCityEvents: List<CityMarketEvent> = emptyList()
    ): Double {
        val safeValuation = max(0L, totalValuation)
        val normalizedValuation = safeValuation / 1_000_000.0

        // 1. Azalan Getiri Değerleme Eğrisi (Sublinear Scale-Free Power-Law + Log Doygunluk)
        val baseValue = (100.0 * normalizedValuation.pow(0.60)) + (20.0 * ln(1.0 + (safeValuation / 500_000.0)))

        // 2. XP / Oyuncu Seviyesi Logaritmik Azalan Getiri (Diminishing Returns Formülü)
        // Formül: xpMultiplier = 1.0 + (alpha * ln(1 + beta * safeXp)) / (1.0 + gamma * ln(1 + safeXp))
        // Bu formül seviye yükseldikçe çarpanı artırır fakat marjinal türevini (artış hızını) yumuşakça sıfıra yaklaştırır.
        val safeXp = max(0, xpProgress)
        val logXp = ln(1.0 + safeXp.toDouble())
        val xpMultiplier = 1.0 + ((1.50 * ln(1.0 + (safeXp * 0.08))) / (1.0 + (0.10 * logXp)))

        // 3. F/K Oranı (P/E Base) ve Likidite Çarpanı Normalizasyonu
        val peFactor = (macroState.peRatioBase.toDouble() / 10.0).coerceIn(0.3, 3.0)
        val liquidityFactor = macroState.marketLiquidityMultiplier.toDouble().coerceIn(0.2, 2.5)

        // 4. Sistem Dinamikleri Şok Sönümleyici (Kocaeli Grevi / CityMarketEvent Enflasyon Filtresi)
        val rawShockDelta = activeCityEvents.filter { !it.isExpired }.sumOf { (it.priceMultiplier - 1.0f).toDouble() }
        // Şok baskısı tanh elastik sönümleme ile sınırlandırılır (+%20 max etki)
        val dampedShockMultiplier = 1.0 + (0.20 * tanh(rawShockDelta / 2.5))

        // 5. Konsorsiyum Kriz Etkisi
        val crisisDropFactor = crisisState?.stockMarketDropMultiplier?.toDouble() ?: 1.0

        // 6. Ham Fiyat (rawPrice) Hesabı: Enflasyon Korumalı Değerleme Algoritması
        val rawPrice = baseValue * xpMultiplier * peFactor * liquidityFactor * dampedShockMultiplier * crisisDropFactor

        // Minimum hisse taban fiyatı 10.0 TL
        return max(10.0, rawPrice)
    }

    /**
     * Borsa Başlangıç/Varsayılan Depo Rezervi (999.999.999 Ton).
     * Supabase sunucusunda çok oyunculu (multiplayer) paylaşılan standart başlangıç stoğu.
     */
    const val DEFAULT_BORSA_STOCK: Long = 999_999_999L

    /**
     * Borsa Kriz Eşiği: Rezerv 999.999 Ton ve altına indiğinde kriz patlak verir ve fiyat 2 katına fırlar!
     */
    const val CRISIS_STOCK_THRESHOLD: Long = 999_999L

    /**
     * Borsa Depo Maksimum Stok Kapasitesi (999 Trilyon Ton).
     * Supabase sunucusunda binlerce oyuncunun ürettiği tüm arzı biriktirebilecek kapasite.
     */
    const val MAX_BORSA_STOCK: Long = 999_999_999_999L

    /**
     * Borsa Ürün Fiyatı ve Depo Stok Arz-Talep Motoru:
     * - Borsadan ürün alındıkça stok azalır ve fiyat alış oranında (%67 artış kuralı dahil) yükselir.
     * - Borsaya ürün satıldıkça stok artar ve borsa spot fiyatı düşer.
     * - Rezerv miktarı 999.999 Ton veya altına düşerse KRİZ DEVREYE GİRER ve fiyat mevcut durumunun 2 KATINA çıkar!
     */
    fun calculatePriceFromStock(
        stock: Long,
        basePrice: Long,
        macroMultiplier: Float = 1.0f
    ): Long {
        val s0 = DEFAULT_BORSA_STOCK.toDouble() // 999_999_999.0
        val safeStock = stock.toDouble().coerceAtLeast(0.0)
        val rawBasePrice = basePrice.coerceAtLeast(10L).toDouble()

        // Temel Alım/Satım Arz-Talep Orantısı:
        // Stok S <= S0 ise: fractionBought = (S0 - S) / S0 -> multiplier = 1.0 + (fractionBought * 2.0)
        // (Örn: 333_333_333_333 alım yapıldığında fraction = 1/3 -> 1.0 + (1/3 * 2.0) = 1.6667 -> +%67 artış)
        // Stok S > S0 ise: satılan ürünler stoğu artırdıkça fiyat düşer -> multiplier = S0 / S (taban 0.20x limitli)
        val supplyDemandMultiplier = if (safeStock <= s0) {
            val fractionBought = ((s0 - safeStock) / s0).coerceIn(0.0, 1.0)
            1.0 + (fractionBought * 2.0)
        } else {
            (s0 / safeStock).coerceIn(0.20, 1.0)
        }

        var computedPrice = (rawBasePrice * supplyDemandMultiplier * macroMultiplier.toDouble()).toLong()

        // Kriz Kontrolü (Stok 999.999 veya altına düştüğünde fiyat 2 katına çıkar):
        if (stock <= CRISIS_STOCK_THRESHOLD) {
            computedPrice *= 2L
        }

        val minPrice = max(1L, (rawBasePrice * 0.20).toLong())
        return computedPrice.coerceAtLeast(minPrice)
    }

    /**
     * Borsadan ürün / hammadde satın alındığında:
     * - Borsa depo stoğu azalır.
     * - Stok azaldığı için fiyat yükselir.
     * - Stok 0 olursa KRİZ ORTAMI devreye girer ve fiyat taban fiyatın 4 katına (4.0x) çıkar.
     */
    fun onBorsaProductPurchased(
        currentPrice: Long,
        currentStock: Long,
        quantityBought: Int,
        basePrice: Long
    ): Pair<Long, Long> {
        val safeQuantity = quantityBought.coerceAtLeast(1).toLong()
        val newStock = (currentStock - safeQuantity).coerceAtLeast(0L)
        val calculatedPrice = calculatePriceFromStock(newStock, basePrice)
        return Pair(max(1L, calculatedPrice), newStock)
    }

    /**
     * Borsaya ürün satıldığında:
     * - Borsa depo stoğu artar (ürünler borsa deposunda birikir).
     * - Stok arttığı için fiyat düşer.
     * - Kriz ortamı varsa (stok 0 idi) satışla birlikte depoya ürün girerek kriz sonlanır ve fiyat normale doğru iner.
     */
    fun onBorsaProductSold(
        currentPrice: Long,
        currentStock: Long,
        quantitySold: Int,
        basePrice: Long
    ): Pair<Long, Long> {
        val safeQuantity = quantitySold.coerceAtLeast(1).toLong()
        val newStock = (currentStock + safeQuantity).coerceAtMost(MAX_BORSA_STOCK)
        val calculatedPrice = calculatePriceFromStock(newStock, basePrice)
        return Pair(max(1L, calculatedPrice), newStock)
    }

    /**
     * Oyuncu veya tesisler ürün ürettiğinde piyasaya yeni arz girer; borsa stoğu artar, fiyat hafifçe gevşer.
     */
    fun onProductManufactured(
        currentPrice: Long,
        currentStock: Long,
        quantityProduced: Int,
        basePrice: Long
    ): Pair<Long, Long> {
        val safeQuantity = quantityProduced.coerceAtLeast(1).toLong()
        val addedStock = (safeQuantity * 0.5).toLong().coerceAtLeast(1L)
        val newStock = (currentStock + addedStock).coerceAtMost(MAX_BORSA_STOCK)
        val calculatedPrice = calculatePriceFromStock(newStock, basePrice)
        return Pair(max(1L, calculatedPrice), newStock)
    }
}
