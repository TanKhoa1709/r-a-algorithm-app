package app.models

import kotlinx.serialization.Serializable

/**
 * Result of a bank transaction (withdraw/deposit)
 */
@Serializable
data class TransactionResult(
    val success: Boolean,
    val message: String,
    val balance: Long
)

