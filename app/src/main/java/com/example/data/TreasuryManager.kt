package com.example.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Company Manager data model representing automation executives.
 */
data class CompanyManagerEntity(
    val id: String,
    val name: String, // e.g., Burak Koç, Ahmet Yılmaz, Ayşe Kaya
    val role: String,
    val dailySalary: Long,
    val isHired: Boolean = false,
    val efficiencyBoost: Float = 0.15f,
    val lastActionTimestampMs: Long = 0L
)

/**
 * Treasury Management Engine with Kotlin [Mutex] synchronization.
 * Guarantees thread-safe atomic access to shared treasury balances when automated managers
 * and player manual transactions attempt to read/modify money concurrently.
 */
class TreasuryManager(initialBalance: Long = 0L) {

    private val treasuryMutex = Mutex()
    private var balanceAmount: Long = initialBalance

    /**
     * Gets current snapshot of treasury balance in thread-safe manner.
     */
    suspend fun getBalance(): Long = treasuryMutex.withLock { balanceAmount }

    /**
     * Atomically sets or resets treasury balance.
     */
    suspend fun setBalance(newBalance: Long) {
        treasuryMutex.withLock {
            balanceAmount = newBalance.coerceAtLeast(0L)
        }
    }

    /**
     * Atomically deposits income to treasury balance.
     */
    suspend fun deposit(amount: Long, sourceDescription: String = ""): Long {
        if (amount <= 0L) return balanceAmount
        return treasuryMutex.withLock {
            balanceAmount += amount
            balanceAmount
        }
    }

    /**
     * Atomically withdraws expenses/purchases from treasury balance.
     * Returns true if transaction succeeds, or false if balance is insufficient.
     */
    suspend fun withdraw(amount: Long, sourceDescription: String = ""): Boolean {
        if (amount <= 0L) return true
        return treasuryMutex.withLock {
            if (balanceAmount >= amount) {
                balanceAmount -= amount
                true
            } else {
                false // Insufficient funds
            }
        }
    }

    /**
     * Executes automated manager action (e.g. Burak Koç purchasing raw materials or paying salaries)
     * guarded by Mutex to prevent double-spending or race conditions.
     */
    suspend fun executeManagerTransaction(
        manager: CompanyManagerEntity,
        cost: Long,
        onApproved: suspend (newBalance: Long) -> Unit
    ): Boolean {
        if (cost <= 0L) return false
        return treasuryMutex.withLock {
            if (balanceAmount >= cost) {
                balanceAmount -= cost
                onApproved(balanceAmount)
                true
            } else {
                false
            }
        }
    }
}
