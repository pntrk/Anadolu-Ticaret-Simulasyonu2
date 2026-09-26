package com.example.data.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Extension helper to reliably extract Activity from any Context/ContextWrapper hierarchy.
 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

object BillingManager : PurchasesUpdatedListener {

    private const val TAG = "BillingManager"

    // Product IDs defined in Google Play Console
    val IN_APP_PRODUCT_IDS = listOf(
        "gem_50",
        "gem_150",
        "gem_500",
        "gem_1500",
        "gem_4000",
        "gem_10000"
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var appContext: Context? = null
    private var billingClient: BillingClient? = null
    private var isConnecting = false
    private val pendingReadyCallbacks = mutableListOf<() -> Unit>()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    private val _isQuerying = MutableStateFlow(false)
    val isQuerying: StateFlow<Boolean> = _isQuerying.asStateFlow()

    private val _lastStatusMessage = MutableStateFlow<String?>(null)
    val lastStatusMessage: StateFlow<String?> = _lastStatusMessage.asStateFlow()

    private val _productsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productsMap: StateFlow<Map<String, ProductDetails>> = _productsMap.asStateFlow()

    /**
     * Direct synchronous/immediate callback for ViewModel or listener.
     */
    var onPurchaseRewardCallback: ((productId: String, gems: Int, orderId: String?, purchaseToken: String) -> Unit)? = null

    // Event emitted when gems purchase is completed & consumed
    sealed class PurchaseEvent {
        data class Success(val productId: String, val gems: Int, val orderId: String?) : PurchaseEvent()
        data class Error(val message: String, val responseCode: Int) : PurchaseEvent()
        data class Message(val message: String) : PurchaseEvent()
    }

    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 50)
    val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    var activePlayerId: String = "player_iap"

    private const val DELIVERED_TOKENS_PREFS = "delivered_iap_purchase_tokens"

    private fun isTokenAlreadyDelivered(context: Context, purchaseToken: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences("iap_rewards", Context.MODE_PRIVATE)
            val set = prefs.getStringSet(DELIVERED_TOKENS_PREFS, emptySet()) ?: emptySet()
            set.contains(purchaseToken)
        } catch (_: Throwable) {
            false
        }
    }

    private fun markTokenDelivered(context: Context, purchaseToken: String) {
        try {
            val prefs = context.getSharedPreferences("iap_rewards", Context.MODE_PRIVATE)
            val set = (prefs.getStringSet(DELIVERED_TOKENS_PREFS, emptySet()) ?: emptySet()).toMutableSet()
            set.add(purchaseToken)
            prefs.edit().putStringSet(DELIVERED_TOKENS_PREFS, set).apply()
        } catch (_: Throwable) {}
    }

    fun getGemsForProduct(productId: String): Int {
        val clean = productId.lowercase().trim()
        return when {
            clean == "gem_10000" || clean.contains("10000") || clean.contains("10k") || clean.contains("empire") -> 15000
            clean == "gem_4000" || clean.contains("4000") || clean.contains("4k") || clean.contains("holding") -> 5500
            clean == "gem_1500" || clean.contains("1500") || clean.contains("ceo") -> 1950
            clean == "gem_500" || clean.contains("500") || clean.contains("growth") -> 600
            clean == "gem_150" || clean.contains("150") || clean.contains("merchant") || clean.contains("tuccar") -> 165
            clean == "gem_50" || clean.contains("50") || clean.contains("starter") -> 50
            else -> {
                val digits = clean.filter { it.isDigit() }.toIntOrNull() ?: 0
                if (digits > 0) digits else 1950
            }
        }
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (billingClient != null) {
            if (billingClient?.isReady == true) {
                _isBillingReady.value = true
                queryProducts()
            } else {
                startConnection()
            }
            return
        }

        val targetContext = appContext ?: context.applicationContext
        val pendingParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(targetContext)
            .setListener(this)
            .enablePendingPurchases(pendingParams)
            .build()

        startConnection()
    }

    fun startConnection(onReady: (() -> Unit)? = null) {
        val client = billingClient ?: run {
            appContext?.let { initialize(it) }
            billingClient
        }

        if (client == null) {
            _lastStatusMessage.value = "Google Play servisi başlatılamadı."
            _isBillingReady.value = false
            return
        }

        if (client.isReady) {
            _isBillingReady.value = true
            queryProducts()
            onReady?.invoke()
            return
        }

        if (onReady != null) {
            synchronized(pendingReadyCallbacks) {
                pendingReadyCallbacks.add(onReady)
            }
        }

        if (isConnecting) return
        isConnecting = true

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                val callbacks = synchronized(pendingReadyCallbacks) {
                    val list = pendingReadyCallbacks.toList()
                    pendingReadyCallbacks.clear()
                    list
                }
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing setup successful.")
                    _isBillingReady.value = true
                    _lastStatusMessage.value = "Google Play Store bağlantısı aktif."
                    queryProducts()
                    queryAndConsumeUnfinishedPurchases()
                    callbacks.forEach { 
                        try {
                            it.invoke()
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error executing pending billing callback", e)
                        }
                    }
                } else {
                    val msg = "Google Play Store bağlantısı kurulamadı (${billingResult.responseCode}): ${billingResult.debugMessage}"
                    Log.w(TAG, msg)
                    _isBillingReady.value = false
                    _lastStatusMessage.value = msg
                    if (callbacks.isNotEmpty()) {
                        _purchaseEvents.tryEmit(
                            PurchaseEvent.Error(
                                "Google Play Store bağlantısı sağlanamadı. Lütfen Play Store ve internet bağlantınızı kontrol edin.",
                                billingResult.responseCode
                            )
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                _isBillingReady.value = false
                _lastStatusMessage.value = "Google Play servisi bağlantısı kesildi."
                Log.w(TAG, "Google Play Billing service disconnected.")
                synchronized(pendingReadyCallbacks) {
                    pendingReadyCallbacks.clear()
                }
            }
        })
    }

    fun queryProducts(onFinished: ((Map<String, ProductDetails>) -> Unit)? = null) {
        val client = billingClient
        if (client == null || !client.isReady) {
            Log.w(TAG, "Cannot query products: BillingClient not ready, attempting reconnect...")
            startConnection {
                queryProducts(onFinished)
            }
            return
        }

        _isQuerying.value = true
        val productList = IN_APP_PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            _isQuerying.value = false
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Determine if queryProductDetailsResult is a List or a wrapped result object
                val list: List<ProductDetails> = if (queryProductDetailsResult is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    queryProductDetailsResult as List<ProductDetails>
                } else {
                    try {
                        val method = queryProductDetailsResult.javaClass.methods.firstOrNull { it.name == "getProductDetailsList" }
                        if (method != null) {
                            @Suppress("UNCHECKED_CAST")
                            (method.invoke(queryProductDetailsResult) as? List<ProductDetails>) ?: emptyList()
                        } else {
                            emptyList()
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error extracting product details list", e)
                        emptyList()
                    }
                }
                
                val map = list.associateBy { it.productId }
                _productsMap.value = map
                Log.d(TAG, "Found ${map.size} products in Google Play: ${map.keys}")
                _lastStatusMessage.value = if (map.isNotEmpty()) {
                    "${map.size} adet Google Play ürünü hazır."
                } else {
                    "Google Play'den 0 ürün döndü. (Play Console ürün durumu 'Etkin' olmalı)"
                }
                onFinished?.invoke(map)
            } else {
                val errorMsg = "Google Play ürün sorgusu başarısız (${billingResult.responseCode}): ${billingResult.debugMessage}"
                _lastStatusMessage.value = errorMsg
                Log.w(TAG, errorMsg)
                onFinished?.invoke(emptyMap())
            }
        }
    }

    /**
     * Launches the official Google Play In-App Billing flow for the given product ID.
     */
    fun launchBillingFlow(
        activity: Activity,
        productId: String,
        onStatusFeedback: ((String) -> Unit)? = null
    ) {
        if (billingClient == null) {
            initialize(activity.applicationContext)
        }

        val client = billingClient
        if (client == null || !client.isReady) {
            Log.w(TAG, "BillingClient is not ready, attempting connection before launching billing flow...")
            onStatusFeedback?.invoke("Google Play Store bağlantısı kuruluyor...")
            startConnection {
                val readyClient = billingClient
                if (readyClient != null && readyClient.isReady) {
                    launchBillingFlow(activity, productId, onStatusFeedback)
                } else {
                    val fallbackMsg = _lastStatusMessage.value ?: "Google Play Store bağlantısı kurulamadı."
                    _purchaseEvents.tryEmit(
                        PurchaseEvent.Error(
                            fallbackMsg,
                            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
                        )
                    )
                }
            }
            return
        }

        val productDetails = _productsMap.value[productId]
        if (productDetails == null) {
            onStatusFeedback?.invoke("Google Play'den $productId ürün detayı sorgulanıyor...")
            queryProducts { updatedMap ->
                val refreshedDetails = updatedMap[productId]
                if (refreshedDetails != null) {
                    launchBillingFlow(activity, productId, onStatusFeedback)
                } else {
                    Log.w(TAG, "Google Play product $productId not returned by Play Store.")
                    _purchaseEvents.tryEmit(PurchaseEvent.Error("Bu ürün şu anda satın alınamıyor.", BillingClient.BillingResponseCode.ITEM_UNAVAILABLE))
                }
            }
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = client.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow returned non-OK code (${result.responseCode}): ${result.debugMessage}")
            _purchaseEvents.tryEmit(PurchaseEvent.Error("Satın alma işlemi başlatılamadı: ${result.debugMessage}", result.responseCode))
        } else {
            onStatusFeedback?.invoke("Google Play satın alma ekranı açılıyor...")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    Log.d(TAG, "Purchases list is null or empty on OK result.")
                    return
                }
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled the purchase flow.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned. Consuming any pending items...")
                queryAndConsumeUnfinishedPurchases()
            }
            else -> {
                Log.w(TAG, "onPurchasesUpdated status: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                _purchaseEvents.tryEmit(
                    PurchaseEvent.Error(
                        billingResult.debugMessage.ifBlank { "Satın alma işlemi tamamlanamadı." },
                        billingResult.responseCode
                    )
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val ctx = appContext
            val token = purchase.purchaseToken
            val alreadyDelivered = if (ctx != null) isTokenAlreadyDelivered(ctx, token) else false

            val productList = if (purchase.products.isNotEmpty()) {
                purchase.products
            } else {
                listOf("gem_1500")
            }

            if (!alreadyDelivered) {
                if (ctx != null) {
                    markTokenDelivered(ctx, token)
                    com.example.data.security.BillingSecurityVerifier.markPurchaseTokenProcessed(ctx, token, purchase.orderId)
                }
                for (productId in productList) {
                    val gems = getGemsForProduct(productId)
                    Log.d(TAG, "Delivering verified purchase reward: $productId -> $gems gems (orderId=${purchase.orderId})")
                    
                    // 1. Async Ledger & Backend Verification
                    if (ctx != null) {
                        scope.launch {
                            com.example.data.security.BillingSecurityVerifier.verifyAndRecordPurchase(
                                context = ctx,
                                purchase = purchase,
                                productId = productId,
                                gems = gems,
                                playerId = activePlayerId
                            )
                        }
                    }

                    // 2. Direct instantaneous callback
                    try {
                        onPurchaseRewardCallback?.invoke(productId, gems, purchase.orderId, token)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error in onPurchaseRewardCallback", e)
                    }

                    // 3. SharedFlow emission
                    scope.launch {
                        _purchaseEvents.emit(
                            PurchaseEvent.Success(
                                productId = productId,
                                gems = gems,
                                orderId = purchase.orderId
                            )
                        )
                    }
                }
            } else {
                Log.d(TAG, "Purchase token $token has already been delivered previously.")
            }

            // Always consume in Google Play so item can be bought again
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient?.consumeAsync(consumeParams) { billingResult, purchaseToken ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase successfully consumed in Google Play: $purchaseToken")
                } else {
                    Log.w(TAG, "Failed to consume purchase in Google Play (${billingResult.responseCode}): ${billingResult.debugMessage}")
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase is pending: ${purchase.orderId}")
            _purchaseEvents.tryEmit(PurchaseEvent.Message("Ödeme onay bekliyor. Onaylandığında elmaslar hesabınıza eklenecektir."))
        }
    }

    /**
     * Checks for any unconsumed completed purchases (e.g. if the app crashed or closed mid-purchase)
     * and consumes them to deliver rewards.
     */
    fun queryAndConsumeUnfinishedPurchases() {
        val client = billingClient
        if (client == null || !client.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(purchase)
                    }
                }
            }
        }
    }
}
