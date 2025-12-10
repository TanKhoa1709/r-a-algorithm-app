package app.node.controller

import app.proto.CSProtocol
import app.proto.MsgType
import app.proto.RAMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Handles incoming messages
 */
@OptIn(ExperimentalSerializationApi::class)
class MessageHandler {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    @OptIn(ExperimentalSerializationApi::class)
    fun handleMessage(
        message: String,
        onRequest: (app.proto.RAMessage) -> Unit,
        onReply: (app.proto.RAMessage) -> Unit,
        onRelease: (app.proto.RAMessage) -> Unit
    ) {
        try {
            val ramessage: RAMessage = runCatching {
                val protocol = json.decodeFromString<CSProtocol>(message)
                json.decodeFromString(protocol.payload)
            }.getOrElse {
                // Fallback: message is already a RAMessage
                json.decodeFromString(message)
            }
            when (ramessage.type) {
                MsgType.REQUEST -> {
                    onRequest(ramessage)
                }
                MsgType.REPLY -> {
                    onReply(ramessage)
                }
                MsgType.RELEASE -> {
                    onRelease(ramessage)
                }
                else -> {
                    // Unknown message type
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

