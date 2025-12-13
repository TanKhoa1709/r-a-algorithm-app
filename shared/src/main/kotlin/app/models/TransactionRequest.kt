package app.models

import kotlinx.serialization.Serializable

/**
 * Request for bank transaction (withdraw/deposit)
 */
@Serializable
data class TransactionRequest(
    val nodeId: String,
    val requestId: String,
    val amount: Long
)

