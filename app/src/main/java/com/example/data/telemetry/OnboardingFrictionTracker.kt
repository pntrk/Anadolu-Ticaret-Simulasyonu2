package com.example.data.telemetry

/**
 * Onboarding Friction Tracker:
 * Specifically dedicated to capturing friction points, UX stumbling blocks,
 * and logical confusion for new players (Level <= 2).
 */
object OnboardingFrictionTracker {

    /**
     * Checks if player is in the early onboarding / adaptation phase (Level <= 2).
     */
    fun isNewPlayer(playerLevel: Int): Boolean = playerLevel <= 2

    /**
     * Captures when a new player tries to purchase or build something without enough funds.
     */
    fun trackInsufficientBalance(
        playerLevel: Int,
        requiredAmount: Long,
        availableAmount: Long,
        purchaseType: String,
        itemId: String
    ) {
        if (!isNewPlayer(playerLevel)) return

        val shortfall = requiredAmount - availableAmount
        TelemetryService.recordEvent(
            eventType = TelemetryEventType.ONBOARDING_FRICTION,
            severity = TelemetrySeverity.INFO,
            tag = "Onboarding_Insufficient_Funds",
            message = "Yeni oyuncu (Lvl $playerLevel) $purchaseType alımında yetersiz bakiye ile karşılaştı: Gerekli=₳$requiredAmount, Mevcut=₳$availableAmount (Açık: ₳$shortfall)",
            playerLevel = playerLevel,
            metadata = mapOf(
                "purchaseType" to purchaseType,
                "itemId" to itemId,
                "required" to requiredAmount.toString(),
                "available" to availableAmount.toString(),
                "shortfall" to shortfall.toString()
            )
        )
    }

    /**
     * Captures when a new player sells products at a severe loss or makes an erroneous trade.
     */
    fun trackUnfavorableTrade(
        playerLevel: Int,
        productId: String,
        avgBuyPrice: Double,
        sellPrice: Double,
        quantity: Int
    ) {
        if (!isNewPlayer(playerLevel)) return
        if (sellPrice >= avgBuyPrice) return // Profitable trade, ignore

        val lossPerUnit = avgBuyPrice - sellPrice
        val totalLoss = (lossPerUnit * quantity).toLong()

        TelemetryService.recordEvent(
            eventType = TelemetryEventType.ONBOARDING_FRICTION,
            severity = TelemetrySeverity.WARN,
            tag = "Onboarding_Unfavorable_Trade",
            message = "Yeni oyuncu (Lvl $playerLevel) zararına satış yaptı: $productId x$quantity (Birim Zarar: ₳${"%.1f".format(lossPerUnit)}, Toplam: ₳$totalLoss)",
            playerLevel = playerLevel,
            metadata = mapOf(
                "productId" to productId,
                "quantity" to quantity.toString(),
                "buyPrice" to avgBuyPrice.toString(),
                "sellPrice" to sellPrice.toString(),
                "totalLoss" to totalLoss.toString()
            )
        )
    }

    /**
     * Captures when a new player reaches warehouse capacity and is blocked from producing/trading.
     */
    fun trackWarehouseOverflow(
        playerLevel: Int,
        currentUsage: Int,
        maxCapacity: Int,
        blockedProductId: String
    ) {
        if (!isNewPlayer(playerLevel)) return

        TelemetryService.recordEvent(
            eventType = TelemetryEventType.ONBOARDING_FRICTION,
            severity = TelemetrySeverity.WARN,
            tag = "Onboarding_Warehouse_Full",
            message = "Yeni oyuncu (Lvl $playerLevel) depo kapasitesi sınırına takıldı: $currentUsage / $maxCapacity Ton. Ürün: $blockedProductId",
            playerLevel = playerLevel,
            metadata = mapOf(
                "currentUsage" to currentUsage.toString(),
                "maxCapacity" to maxCapacity.toString(),
                "blockedProductId" to blockedProductId
            )
        )
    }

    /**
     * Captures when a new player dismisses a key tutorial/help dialog in less than 2 seconds (confusion or accidental tap).
     */
    fun trackRapidDialogDismissal(
        playerLevel: Int,
        dialogName: String,
        openDurationMs: Long
    ) {
        if (!isNewPlayer(playerLevel)) return
        if (openDurationMs > 2500L) return

        TelemetryService.recordEvent(
            eventType = TelemetryEventType.ONBOARDING_FRICTION,
            severity = TelemetrySeverity.INFO,
            tag = "Onboarding_Rapid_Dialog_Dismiss",
            message = "Yeni oyuncu (Lvl $playerLevel) $dialogName diyaloğunu çok hızlı kapattı (${openDurationMs}ms). Olası kafa karışıklığı veya yanlış dokunma.",
            playerLevel = playerLevel,
            metadata = mapOf(
                "dialogName" to dialogName,
                "openDurationMs" to openDurationMs.toString()
            )
        )
    }

    /**
     * Captures illegal/negative balance manipulation or arithmetic edge case attempts.
     */
    fun trackLogicalEdgeCase(
        playerLevel: Int,
        context: String,
        action: String,
        details: Map<String, String>
    ) {
        TelemetryService.recordEvent(
            eventType = TelemetryEventType.LOGICAL_EDGE_CASE,
            severity = TelemetrySeverity.WARN,
            tag = "Logical_Edge_Case",
            message = "Mantıksal sınır veya uç durum yakalandı: $context - $action (Lvl $playerLevel)",
            playerLevel = playerLevel,
            metadata = details
        )
    }
}
