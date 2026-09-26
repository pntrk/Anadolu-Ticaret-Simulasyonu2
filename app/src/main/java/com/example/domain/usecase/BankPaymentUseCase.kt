package com.example.domain.usecase

import com.example.data.GameRepository
import com.example.data.PlayerEntity
import com.example.data.security.AntiCheatEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Bank & Loan Settlement Use Case.
 * Handles credit repayments, interest payments, and liquidity settlement.
 */
class BankPaymentUseCase(
    private val repository: GameRepository
) {
    sealed class PaymentResult {
        data class Success(val updatedPlayer: PlayerEntity, val paidAmount: Long, val isFullyPaid: Boolean) : PaymentResult()
        data class InsufficientFunds(val required: Long, val available: Long) : PaymentResult()
        object NoLoanToPay : PaymentResult()
    }

    suspend fun repayLoan(player: PlayerEntity, amount: Long): PaymentResult = withContext(Dispatchers.IO) {
        if (player.loanAmount <= 0L) {
            return@withContext PaymentResult.NoLoanToPay
        }

        val actualPayAmount = amount.coerceAtMost(player.loanAmount)
        if (player.money < actualPayAmount) {
            return@withContext PaymentResult.InsufficientFunds(required = actualPayAmount, available = player.money)
        }

        val newLoan = (player.loanAmount - actualPayAmount).coerceAtLeast(0L)
        val newMoney = (player.money - actualPayAmount).coerceAtLeast(0L)

        val updatedPlayer = AntiCheatEngine.sanitizePlayer(
            player.copy(
                money = newMoney,
                loanAmount = newLoan
            )
        )

        repository.updatePlayer(updatedPlayer)

        PaymentResult.Success(
            updatedPlayer = updatedPlayer,
            paidAmount = actualPayAmount,
            isFullyPaid = newLoan == 0L
        )
    }

    suspend fun depositMoney(player: PlayerEntity, amount: Long): PaymentResult = withContext(Dispatchers.IO) {
        if (amount <= 0L || player.money < amount) {
            return@withContext PaymentResult.InsufficientFunds(required = amount, available = player.money)
        }

        val updatedPlayer = AntiCheatEngine.sanitizePlayer(
            player.copy(
                money = player.money - amount,
                depositBalance = player.depositBalance + amount
            )
        )
        repository.updatePlayer(updatedPlayer)
        PaymentResult.Success(updatedPlayer, amount, false)
    }

    suspend fun withdrawDeposit(player: PlayerEntity, amount: Long): PaymentResult = withContext(Dispatchers.IO) {
        val actualWithdraw = amount.coerceAtMost(player.depositBalance)
        if (actualWithdraw <= 0L) {
            return@withContext PaymentResult.InsufficientFunds(required = amount, available = player.depositBalance)
        }

        val updatedPlayer = AntiCheatEngine.sanitizePlayer(
            player.copy(
                money = player.money + actualWithdraw,
                depositBalance = player.depositBalance - actualWithdraw
            )
        )
        repository.updatePlayer(updatedPlayer)
        PaymentResult.Success(updatedPlayer, actualWithdraw, false)
    }
}
