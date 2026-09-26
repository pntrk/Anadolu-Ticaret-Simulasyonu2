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
    private val smartNotificationManager: SmartNotificationManager
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
        val baseDuration = 10000L
        val durationMs = (baseDuration / business.level.coerceAtLeast(1).toFloat()).toLong()
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
            val updatedBusiness = business.withAddedItem(product.id, produceQty)
            repository.updateBusiness(updatedBusiness)
            removeProgress(uniqueProcessId)
            
            smartNotificationManager.show(
                message = "Üretim Tamamlandı: ${product.getDisplayName()} x$produceQty tesis deposuna eklendi.",
                enMessage = "Production Completed: ${product.getDisplayName()} x$produceQty added to facility warehouse.",
                type = NotificationType.SUCCESS
            )
        }
    }
}
