package app.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Protocol wrapper for messages with type information
 */
@Serializable
data class CSProtocol(
    val type: String,
    val payload: String,
    val timestamp: Long,
    val sourceNodeId: String
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        
        fun fromMessage(message: RAMessage): CSProtocol {
            val payload = json.encodeToString(RAMessage.serializer(), message)
            return CSProtocol(
                type = message.type.name,
                payload = payload,
                timestamp = message.timestamp,
                sourceNodeId = message.nodeId
            )
        }
    }
}

