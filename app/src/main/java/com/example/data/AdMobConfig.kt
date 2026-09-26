package com.example.data

/**
 * Google AdMob Reklam Yapılandırması (AdMob Configuration)
 * 
 * Google Mobile Ads SDK ile %100 Uyumlu Gerçek ve Test AdMob İlan Kimlikleri
 */
object AdMobConfig {
    // Google AdMob Uygulama Kimliği (App ID - Yayıncı: ca-app-pub-5862795194815672)
    var ADMOB_APP_ID: String = "ca-app-pub-5862795194815672~1855266436"

    // Google AdMob Resmi Test Ödüllü Reklam Birim Kimliği (Google Sample Rewarded Ad Unit ID)
    const val TEST_REWARDED_AD_UNIT_ID: String = "ca-app-pub-3940256099942544/5224354917"

    // Kullanıcının Yayıncı Ödüllü Reklam Birim Kimliği (Rewarded Video Ad Unit ID)
    var PROD_REWARDED_AD_UNIT_ID: String = "ca-app-pub-5862795194815672/1635774587"

    // Otomatik Test Reklamına Geçiş (Eğer prod reklam birimi no-fill veya onay bekliyorsa test reklamı gösterilir)
    var autoFallbackToTestAds: Boolean = true

    // Manuel test modu seçeneği
    var useTestAds: Boolean = false

    val REWARDED_AD_UNIT_ID: String
        get() = if (useTestAds) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID

    // Reklam İzleme Başına Verilecek Elmas Miktarı
    const val REWARD_GEMS_AMOUNT: Int = 5
}

