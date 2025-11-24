package app.proto

import kotlinx.serialization.Serializable

/**
 * Unified message type for Ricart-Agrawala algorithm communication
 */
@Serializable
data class RAMessage(
    val type: MsgType,
    val timestamp: Long,
    val nodeId: String,
    val requestId: String
)

