package app.proto

import kotlinx.serialization.Serializable

/**
 * Base message type for Ricart-Agrawala algorithm communication
 */
@Serializable
sealed class RAMessage {
    abstract val timestamp: Long
    abstract val nodeId: String
}

/**
 * Request message to enter critical section
 */
@Serializable
data class RequestMessage(
    override val timestamp: Long,
    override val nodeId: String,
    val requestId: String
) : RAMessage()

/**
 * Reply message granting permission to enter critical section
 */
@Serializable
data class ReplyMessage(
    override val timestamp: Long,
    override val nodeId: String,
    val requestId: String
) : RAMessage()

/**
 * Release message indicating exit from critical section
 */
@Serializable
data class ReleaseMessage(
    override val timestamp: Long,
    override val nodeId: String,
    val requestId: String
) : RAMessage()

