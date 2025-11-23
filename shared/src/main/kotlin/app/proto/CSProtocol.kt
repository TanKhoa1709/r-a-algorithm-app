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
        private val json = Json { }
        
        fun fromMessage(message: RAMessage, type: MsgType): CSProtocol {
            val payload = when (message) {
                is RequestMessage -> json.encodeToString(RequestMessage.serializer(), message)
                is ReplyMessage -> json.encodeToString(ReplyMessage.serializer(), message)
                is ReleaseMessage -> json.encodeToString(ReleaseMessage.serializer(), message)
            }
            return CSProtocol(
                type = type.name,
                payload = payload,
                timestamp = message.timestamp,
                sourceNodeId = message.nodeId
            )
        }
    }
}

