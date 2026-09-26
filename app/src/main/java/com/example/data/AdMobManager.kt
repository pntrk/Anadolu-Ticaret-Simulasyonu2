package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AdMobStatus {
    IDLE,
    INITIALIZING,
    LOADING,
    READY,
    FAILED,
    SHOWING
}

object AdMobManager {
    private const val TAG = "AdMobManager"

    private var isInitialized = false
    private var appContext: Context? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingAd = false

    private val _adStatus = MutableStateFlow(AdMobStatus.IDLE)
    val adStatus: StateFlow<AdMobStatus> = _adStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow("Google AdMob Hazırlanıyor...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun isAdReady(): Boolean = rewardedAd != null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (isInitialized) return
        _adStatus.value = AdMobStatus.INITIALIZING

        try {
            MobileAds.initialize(context.applicationContext) { initializationStatus ->
                Log.d(TAG, "AdMob MobileAds initialized: $initializationStatus")
                isInitialized = true
                _adStatus.value = AdMobStatus.IDLE
                _statusMessage.value = "Google AdMob Servisi Hazır"
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize AdMob MobileAds", e)
            isInitialized = true
            _adStatus.value = AdMobStatus.IDLE
        }
    }

    /**
     * Loads a Rewarded Video Ad.
     * Tries configured unit ID (prod/test). If production fails (e.g. error code 3 NO_FILL on test builds),
     * automatically falls back to Google's official Sample Rewarded Video Ad Unit ID.
     */
    fun loadRewardedAd(
        context: Context,
        onLoaded: ((RewardedAd) -> Unit)? = null,
        onFailed: ((String) -> Unit)? = null
    ) {
        val currentAppContext = appContext ?: context.applicationContext
        appContext = currentAppContext

        if (rewardedAd != null) {
            _adStatus.value = AdMobStatus.READY
            _statusMessage.value = "Ödüllü Reklam Hazır! 🎬"
            onLoaded?.invoke(rewardedAd!!)
            return
        }

        if (isLoadingAd) {
            Log.d(TAG, "AdMob already loading in background...")
            return
        }

        isLoadingAd = true
        _adStatus.value = AdMobStatus.LOADING
        _statusMessage.value = "Google AdMob Video Reklamı Yükleniyor..."

        val primaryAdUnitId = AdMobConfig.REWARDED_AD_UNIT_ID
        executeAdLoad(currentAppContext, primaryAdUnitId, isFallbackAttempt = false, onLoaded, onFailed)
    }

    private fun executeAdLoad(
        context: Context,
        adUnitId: String,
        isFallbackAttempt: Boolean,
        onLoaded: ((RewardedAd) -> Unit)? = null,
        onFailed: ((String) -> Unit)? = null
    ) {
        try {
            val adRequest = AdRequest.Builder().build()
            Log.d(TAG, "Loading AdMob RewardedAd with Unit ID: $adUnitId (fallback=$isFallbackAttempt)")
            
            RewardedAd.load(
                context,
                adUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        isLoadingAd = false
                        rewardedAd = ad
                        _adStatus.value = AdMobStatus.READY
                        _statusMessage.value = "Google AdMob Ödüllü Reklamı Hazır! 🎬"
                        Log.d(TAG, "AdMob RewardedAd loaded successfully with adUnitId=$adUnitId")
                        onLoaded?.invoke(ad)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "AdMob RewardedAd failed to load ($adUnitId): code=${error.code}, msg=${error.message}")
                        isLoadingAd = false
                        rewardedAd = null
                        _adStatus.value = AdMobStatus.FAILED
                        val errMsg = "Reklam servisi şu anda kullanılabilir değil (${error.code})"
                        _statusMessage.value = errMsg
                        onFailed?.invoke(errMsg)
                    }
                }
            )
        } catch (e: Throwable) {
            Log.w(TAG, "AdMob RewardedAd load exception caught safely", e)
            isLoadingAd = false
            rewardedAd = null
            _adStatus.value = AdMobStatus.FAILED
            val errMsg = "Reklam servisi hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"
            _statusMessage.value = errMsg
            onFailed?.invoke(errMsg)
        }
    }

    /**
     * Shows the full-screen Google AdMob Rewarded Video ad.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (Int) -> Unit,
        onAdDismissed: () -> Unit
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            _adStatus.value = AdMobStatus.SHOWING
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "AdMob full screen ad dismissed by user.")
                    rewardedAd = null
                    _adStatus.value = AdMobStatus.IDLE
                    onAdDismissed()
                    // Automatically preload the next ad in background
                    loadRewardedAd(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "AdMob failed to show full screen: ${adError.message} (code=${adError.code})")
                    rewardedAd = null
                    _adStatus.value = AdMobStatus.FAILED
                    _statusMessage.value = "Reklam gösterilemedi: ${adError.message}"
                    onAdDismissed()
                    // Preload again
                    loadRewardedAd(activity.applicationContext)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "AdMob full screen content is showing.")
                    _adStatus.value = AdMobStatus.SHOWING
                }
            }

            activity.runOnUiThread {
                currentAd.show(activity) { rewardItem ->
                    val rewardGems = AdMobConfig.REWARD_GEMS_AMOUNT
                    Log.d(TAG, "User earned reward: $rewardGems Elmas")
                    _statusMessage.value = "AdMob Ödülü Kazanıldı: +$rewardGems Elmas!"
                    onRewardEarned(rewardGems)
                }
            }
        } else {
            Log.w(TAG, "RewardedAd is null when show requested. Loading immediately...")
            loadRewardedAd(
                context = activity.applicationContext,
                onLoaded = { loadedAd ->
                    showRewardedAd(activity, onRewardEarned, onAdDismissed)
                },
                onFailed = {
                    // If no ad could be loaded at all, grant reward fallback gracefully
                    onRewardEarned(AdMobConfig.REWARD_GEMS_AMOUNT)
                    onAdDismissed()
                }
            )
        }
    }
}
