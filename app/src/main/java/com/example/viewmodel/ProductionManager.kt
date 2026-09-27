package com.example.viewmodel

import com.example.data.GameRepository
import com.example.data.BusinessEntity
import com.example.data.Product
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay

class ProductionManager(
    private val repository: GameRepository,
    private val smartNotificationManager: SmartNotificationManager,
    private val onProductionCompleted: ((Int) -> Unit)? = null
) {
    private val _productionProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val productionProgress: StateFlow<Map<String, Float>> = _productionProgress.asStateFlow()

    private val _productionDurations = MutableStateFlow<Map<String, Long>>(emptyMap())
    val productionDurations: StateFlow<Map<String, Long>> = _productionDurations.asStateFlow()

    private val _productionSkip = MutableStateFlow<Set<String>>(emptySet())
    val productionSkip: StateFlow<Set<String>> = _productionSkip.asStateFlow()

    fun updateProgress(businessId: String, progress: Float) {
        val currentMap = _productionProgress.value.toMutableMap()
        currentMap[businessId] = progress
        _productionProgress.value = currentMap
    }

    fun removeProgress(businessId: String) {
        val currentMap = _productionProgress.value.toMutableMap()
        currentMap.remove(businessId)
        _productionProgress.value = currentMap
        
        val skipMap = _productionSkip.value.toMutableSet()
        skipMap.remove(businessId)
        _productionSkip.value = skipMap
    }

    fun setDuration(businessId: String, durationMs: Long) {
        val currentMap = _productionDurations.value.toMutableMap()
        currentMap[businessId] = durationMs
        _productionDurations.value = currentMap
    }

    fun skipProduction(businessId: String) {
        val currentSet = _productionSkip.value.toMutableSet()
        currentSet.add(businessId)
        _productionSkip.value = currentSet
    }

    suspend fun startProduction(
        business: BusinessEntity,
        product: Product,
        isAutoProduceActive: Boolean
    ) {
        if (business.getRemainingStorageCapacity() <= 0) {
            smartNotificationManager.show(
                message = "⚠️ Depo Dolu: ${business.type} tesisi deposunda yer kalmadı. Depo dolunca üretim durur, deponun boşalması beklenir.",
                enMessage = "⚠️ Storage Full: No space in ${business.type} warehouse. Production stopped until storage is cleared.",
                type = NotificationType.ALERT
            )
            return
        }

        val baseDuration = 10000L
        val baseDurationMs = (baseDuration / business.level.coerceAtLeast(1).toFloat()).toLong()
        val durationMs = (baseDurationMs * com.example.data.QualityCraftingService.getWearDurationMultiplier(business.wearLevel)).toLong()
        val businessId = business.id.toString()
        val uniqueProcessId = "${businessId}_${System.currentTimeMillis()}"

        setDuration(uniqueProcessId, durationMs)

        var elapsed = 0L
        val interval = 250L
        var skipped = false

        while (elapsed < durationMs) {
            delay(interval)
            
            if (_productionSkip.value.contains(uniqueProcessId)) {
                skipped = true
                break
            }
            
            elapsed += interval
            updateProgress(uniqueProcessId, elapsed.toFloat() / durationMs)
        }

        if (skipped || elapsed >= durationMs) {
            val produceQty = business.level
            val usedQualities = mutableListOf<com.example.data.ItemQuality>()
            var updatedBusiness = business

            if (product.recipe.isNotEmpty()) {
                val storedMap = business.getStoredItemsMap()
                for (req in product.recipe) {
                    val needed = req.amountPerUnit * produceQty
                    val matchingKeys = storedMap.keys.filter { com.example.data.ItemQuality.extractBaseProductId(it) == req.productId }
                    var remaining = needed
                    for (key in matchingKeys) {
                        if (remaining <= 0) break
                        val avail = storedMap[key] ?: 0
                        if (avail > 0) {
                            val take = minOf(avail, remaining)
                            updatedBusiness = updatedBusiness.withRemovedItem(key, take)
                            val q = com.example.data.ItemQuality.extractQuality(key)
                            repeat(minOf(take, 10)) { usedQualities.add(q) }
                            remaining -= take
                        }
                    }
                    if (remaining > 0) {
                        repeat(minOf(remaining, 10)) {
                            usedQualities.add(com.example.data.ItemQuality.STAR_1)
                        }
                    }
                }
            }

            val itemQuality = com.example.data.QualityCraftingService.calculateProducedItemQuality(
                product = product,
                facilityLevel = business.level,
                usedIngredientQualities = usedQualities,
                wearLevel = business.wearLevel
            )
            val newWear = (updatedBusiness.wearLevel + (0.0005f * produceQty)).coerceAtMost(1.0f)
            val finalBusiness = updatedBusiness.copy(wearLevel = newWear).withAddedItemWithQuality(product.id, itemQuality, produceQty)
            repository.updateBusiness(finalBusiness)
            onProductionCompleted?.invoke(produceQty)
            removeProgress(uniqueProcessId)
            
            smartNotificationManager.show(
                message = "Üretim Tamamlandı: [${itemQuality.starsText} ${itemQuality.label}] ${product.getDisplayName()} x$produceQty tesis deposuna eklendi.",
                enMessage = "Production Completed: [${itemQuality.starsText} ${itemQuality.label}] ${product.getDisplayName()} x$produceQty added to facility warehouse.",
                type = NotificationType.SUCCESS
            )
        }
    }
}

