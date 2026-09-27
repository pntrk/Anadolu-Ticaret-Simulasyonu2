package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.ui.components.NotificationType
import com.example.ui.components.SmartNotificationManager
import com.example.ui.components.formatCredit
import com.example.ui.components.formatMoney
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Breakdown of corporate loan daily installment
 */
data class LoanInstallmentBreakdown(
    val principalInstallment: Long,
    val dailyInterestCost: Long,
    val totalDailyInstallment: Long,
    val remainingDaysEstimate: Int
)

/**
 * Receipt resulting from daily banking settlement
 */
data class BankDailySettlementReceipt(
    val daysSettled: Int,
    val totalDepositYieldEarned: Long,
    val totalLoanInstallmentPaid: Long,
    val totalPrincipalRepaid: Long,
    val totalInterestCharged: Long,
    val initialMoney: Long,
    val finalMoney: Long,
    val initialDeposit: Long,
    val finalDeposit: Long,
    val initialLoan: Long,
    val finalLoan: Long,
    val isShortfall: Boolean
)

/**
 * Gerçek zamanlı gün sonu (00:00) senkronizasyonlu
 * Vadeli Mevduat Günlük Getiri ve Kurumsal Kredi Taksit Yönetim Motoru.
 */
object BankDailySettlementManager {
    private const val TAG = "BankDailySettlement"
    private const val PREFS_NAME = "anadolu_bank_settlement_prefs"
    private const val KEY_LAST_SETTLED_DAY = "key_last_settled_day_str"
    private const val KEY_LAST_SETTLED_TIMESTAMP = "key_last_settled_timestamp_ms"
    private const val DEFAULT_LOAN_TERM_DAYS = 30

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val dateFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Mevcut gerçek zamanlı gün stringi (Örn: 2026-08-27)
     */
    fun getCurrentDayString(): String {
        return dateFormat.format(Date(com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()))
    }

    /**
     * Bir sonraki gece yarısına (00:00:00) kalan milisaniye süresi
     */
    fun getRemainingMillisUntilNextMidnight(): Long {
        val now = com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val nextMidnight = calendar.timeInMillis
        return (nextMidnight - now).coerceAtLeast(0L)
    }

    /**
     * Vadeli mevduatın 1 gerçek zamanlı gün içindeki net getirisini hesaplar.
     * Günlük Faiz = Mevduat Bakiyesi * (Yıllık Politika Faizi / 365)
     */
    /**
     * Vadeli mevduatın 1 gerçek zamanlı gün içindeki net getirisini hesaplar.
     * Günlük Faiz = Mevduat Bakiyesi * (Yıllık Politika Faizi / 365)
     */
    fun calculateDailyDepositYield(
        depositBalance: Long,
        annualDepositRate: Float,
        activeArtifactBuffs: Map<ArtifactBuffType, Float> = emptyMap()
    ): Long {
        if (depositBalance <= 0L) return 0L
        val depositBonus = activeArtifactBuffs[ArtifactBuffType.DEPOSIT_INTEREST_BONUS] ?: 0f
        val boostedRate = annualDepositRate + depositBonus
        val effectiveRate = if (boostedRate <= 0.01f) 0.15f else boostedRate
        return (depositBalance * (effectiveRate / 365.0f)).toLong().coerceAtLeast(0L)
    }

    /**
     * Kurumsal kredi taksit dökümünü hesaplar.
     * Kurumsal krediler 30 günlük günlük taksit planına göre amorti edilir.
     * Günlük Faiz = Anapara Borcu * (Yıllık Kredi Faizi / 365)
     * Günlük Anapara Taksiti = Anapara Borcu / 30 gün
     * Toplam Günlük Taksit = Anapara Taksiti + Günlük Faiz
     */
    fun calculateDailyLoanInstallment(
        loanAmount: Long,
        annualLoanRate: Float,
        activeArtifactBuffs: Map<ArtifactBuffType, Float> = emptyMap()
    ): LoanInstallmentBreakdown {
        if (loanAmount <= 0L) {
            return LoanInstallmentBreakdown(
                principalInstallment = 0L,
                dailyInterestCost = 0L,
                totalDailyInstallment = 0L,
                remainingDaysEstimate = 0
            )
        }

        val loanDiscount = activeArtifactBuffs[ArtifactBuffType.LOAN_INTEREST_DISCOUNT] ?: 0f
        val discountedRate = (annualLoanRate - loanDiscount).coerceAtLeast(0.0f)
        val effectiveRate = if (discountedRate <= 0.01f) 0.01f else discountedRate
        val dailyInterest = (loanAmount * (effectiveRate / 365.0f)).toLong().coerceAtLeast(1L)
        val dailyPrincipal = ((loanAmount + DEFAULT_LOAN_TERM_DAYS - 1) / DEFAULT_LOAN_TERM_DAYS)
            .coerceIn(100L, loanAmount)
        val totalInstallment = dailyPrincipal + dailyInterest
        val remainingDays = if (dailyPrincipal > 0) ((loanAmount + dailyPrincipal - 1) / dailyPrincipal).toInt() else 0

        return LoanInstallmentBreakdown(
            principalInstallment = dailyPrincipal,
            dailyInterestCost = dailyInterest,
            totalDailyInstallment = totalInstallment,
            remainingDaysEstimate = remainingDays.coerceAtLeast(1)
        )
    }

    /**
     * Gerçek zamanlı gün mutabakatı kontrolü ve uygulanması.
     * Uygulama açıkken veya çevrimdışından dönüldüğünde çağrılır.
     * Son mutabakat gününden bu yana geçen her gerçek takvim günü için:
     * 1. Vadeli mevduat günlük faizi hesaplanır ve mevduata (bileşik) eklenir.
     * 2. Kurumsal kredi günlük taksiti (anapara + faiz) nakitten (yetmezse mevduattan) tahsil edilir ve kredi borcu düşürülür.
     */
    fun performDailySettlementIfDue(
        context: Context,
        player: PlayerEntity,
        annualDepositRate: Float,
        annualLoanRate: Float,
        activeArtifactBuffs: Map<ArtifactBuffType, Float>,
        isEnglish: Boolean = false
    ): Pair<PlayerEntity, BankDailySettlementReceipt?> {
        val prefs = getPrefs(context)
        val todayStr = getCurrentDayString()
        val lastSettledDay = prefs.getString(KEY_LAST_SETTLED_DAY, null)

        // İlk kurulum / ilk çalıştırma ise bugünü başlangıç olarak kaydet ve çık
        if (lastSettledDay == null) {
            prefs.edit()
                .putString(KEY_LAST_SETTLED_DAY, todayStr)
                .putLong(KEY_LAST_SETTLED_TIMESTAMP, com.example.data.security.TimeSecurityManager.getSecureCurrentTimeMs())
                .apply()
            Log.d(TAG, "Initial bank settlement anchor set to: $todayStr")
            return Pair(player, null)
        }

        // Eğer bugün zaten mutabakat yapıldıysa işlem yapma
        if (lastSettledDay == todayStr) {
            return Pair(player, null)
        }

        // Kaç takvim günü geçtiğini hesapla
        val daysElapsed = try {
            val lastDate = dateFormat.parse(lastSettledDay)
            val todayDate = dateFormat.parse(todayStr)
            if (lastDate != null && todayDate != null) {
                val diffMs = todayDate.time - lastDate.time
                (diffMs / (24 * 60 * 60 * 1000L)).toInt().coerceIn(1, 30) // En fazla 30 günlük çevrimdışı mutabakat
            } else 1
        } catch (e: Exception) {
            Log.e(TAG, "Date parse error in settlement: ${e.message}")
            1
        }

        if (daysElapsed <= 0) {
            return Pair(player, null)
        }

        Log.d(TAG, "Executing bank settlement for $daysElapsed day(s) between $lastSettledDay and $todayStr")

        var curMoney = player.money
        var curDeposit = player.depositBalance
        var curLoan = player.loanAmount

        var totalDepositYield = 0L
        var totalLoanPaid = 0L
        var totalPrincipalRepaid = 0L
        var totalInterestCharged = 0L
        var isShortfall = false

        for (day in 1..daysElapsed) {
            // 1. Günlük Vadeli Mevduat Getirisi (Bileşik büyüme)
            if (curDeposit > 0L) {
                val dayYield = calculateDailyDepositYield(curDeposit, annualDepositRate, activeArtifactBuffs)
                curDeposit += dayYield
                totalDepositYield += dayYield
            }

            // 2. Günlük Kurumsal Kredi Taksiti Tahsili
            if (curLoan > 0L) {
                val installmentBreakdown = calculateDailyLoanInstallment(curLoan, annualLoanRate, activeArtifactBuffs)
                val interestDue = installmentBreakdown.dailyInterestCost
                val principalDue = installmentBreakdown.principalInstallment
                val totalInstallmentDue = principalDue + interestDue

                totalInterestCharged += interestDue

                // Ödeme önceliği: Nakit -> Vadeli Mevduat
                if (curMoney >= totalInstallmentDue) {
                    curMoney -= totalInstallmentDue
                    curLoan = (curLoan - principalDue).coerceAtLeast(0L)
                    totalLoanPaid += totalInstallmentDue
                    totalPrincipalRepaid += principalDue
                } else {
                    val availableLiquid = curMoney
                    val shortfall = totalInstallmentDue - availableLiquid

                    if (curDeposit >= shortfall) {
                        // Nakiti sıfırla, kalanı mevduattan al
                        curMoney = 0L
                        curDeposit -= shortfall
                        curLoan = (curLoan - principalDue).coerceAtLeast(0L)
                        totalLoanPaid += totalInstallmentDue
                        totalPrincipalRepaid += principalDue
                    } else {
                        // Nakit ve mevduat yetersiz kaldı
                        val allFunds = curMoney + curDeposit
                        curMoney = 0L
                        curDeposit = 0L
                        isShortfall = true

                        if (allFunds >= interestDue) {
                            // En azından faizi öde, kalanı anaparadan düş
                            val remainingForPrincipal = allFunds - interestDue
                            curLoan = (curLoan - remainingForPrincipal).coerceAtLeast(0L)
                            totalLoanPaid += allFunds
                            totalPrincipalRepaid += remainingForPrincipal
                        } else {
                            // Faiz dahi ödenemedi, ödenemeyen faiz anaparaya eklendi
                            val unpaidInterest = interestDue - allFunds
                            curLoan += unpaidInterest
                            totalLoanPaid += allFunds
                        }
                    }
                }
            }
        }

        // Yeni mutabakat tarihini kaydet
        prefs.edit()
            .putString(KEY_LAST_SETTLED_DAY, todayStr)
            .putLong(KEY_LAST_SETTLED_TIMESTAMP, System.currentTimeMillis())
            .apply()

        val updatedPlayer = player.copy(
            money = curMoney,
            depositBalance = curDeposit,
            loanAmount = curLoan
        )

        val receipt = BankDailySettlementReceipt(
            daysSettled = daysElapsed,
            totalDepositYieldEarned = totalDepositYield,
            totalLoanInstallmentPaid = totalLoanPaid,
            totalPrincipalRepaid = totalPrincipalRepaid,
            totalInterestCharged = totalInterestCharged,
            initialMoney = player.money,
            finalMoney = curMoney,
            initialDeposit = player.depositBalance,
            finalDeposit = curDeposit,
            initialLoan = player.loanAmount,
            finalLoan = curLoan,
            isShortfall = isShortfall
        )

        return Pair(updatedPlayer, receipt)
    }

    /**
     * Manuel tek günlük taksit ödemesi (Erken ödeme)
     */
    fun payManualDailyInstallment(
        player: PlayerEntity,
        annualLoanRate: Float
    ): Pair<PlayerEntity, Boolean> {
        if (player.loanAmount <= 0L) return Pair(player, false)

        val breakdown = calculateDailyLoanInstallment(player.loanAmount, annualLoanRate)
        val totalDue = breakdown.totalDailyInstallment
        val principal = breakdown.principalInstallment

        if (player.money >= totalDue) {
            val newMoney = player.money - totalDue
            val newLoan = (player.loanAmount - principal).coerceAtLeast(0L)
            return Pair(player.copy(money = newMoney, loanAmount = newLoan), true)
        } else if (player.money + player.depositBalance >= totalDue) {
            val fromMoney = player.money
            val fromDeposit = totalDue - fromMoney
            val newDeposit = player.depositBalance - fromDeposit
            val newLoan = (player.loanAmount - principal).coerceAtLeast(0L)
            return Pair(player.copy(money = 0L, depositBalance = newDeposit, loanAmount = newLoan), true)
        }
        return Pair(player, false)
    }
}
