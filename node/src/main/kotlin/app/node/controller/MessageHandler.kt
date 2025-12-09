package app.node.controller

import app.proto.CSProtocol
import app.proto.MsgType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Handles incoming messages
 */
class MessageHandler {
    fun handleMessage(
        message: String,
        onRequest: (app.proto.RAMessage) -> Unit,
        onReply: (app.proto.RAMessage) -> Unit,
        onRelease: (app.proto.RAMessage) -> Unit
    ) {
        try {
            val protocol = Json.decodeFromString<CSProtocol>(message)
            val ramessage = Json.decodeFromString<app.proto.RAMessage>(protocol.payload)
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

