package app.models

import kotlinx.serialization.Serializable

/**
 * Represents an entry in the critical section access log
 */
@Serializable
data class CSEntry(
    val nodeId: String,
    val requestId: String,
    val timestamp: Long,
    val entryTime: Long,
    val exitTime: Long? = null,
    val duration: Long? = null,
    val transactionType: String? = null,  // "WITHDRAW" or "DEPOSIT"
    val amount: Long? = null,  // Transaction amount
    val balance: Long? = null  // Balance after transaction
)

