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
    val duration: Long? = null
)

