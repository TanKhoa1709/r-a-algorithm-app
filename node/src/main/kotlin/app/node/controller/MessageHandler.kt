package app.node.controller

import app.proto.CSProtocol
import app.proto.MsgType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Handles incoming messages
 */
class MessageHandler {
    fun handleMessage(message: String, onRequest: (app.proto.RequestMessage) -> Unit) {
        try {
            val protocol = Json.decodeFromString<CSProtocol>(message)
            when (MsgType.valueOf(protocol.type)) {
                MsgType.REQUEST -> {
                    val request = Json.decodeFromString<app.proto.RequestMessage>(protocol.payload)
                    onRequest(request)
                }
                MsgType.REPLY -> {
                    // Handle reply
                }
                MsgType.RELEASE -> {
                    // Handle release
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

