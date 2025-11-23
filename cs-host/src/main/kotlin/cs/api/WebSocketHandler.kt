package cs.api

import cs.CSHost
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WebSocket handler for real-time CS Host updates
 */
fun Application.configureWebSocketHandler(csHost: CSHost) {
    routing {
        webSocket("/ws/cs-host") {
            // Send initial state
            val state = csHost.getState()
            send(Frame.Text(Json.encodeToString(state)))
            
            // Handle incoming messages
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val message = frame.readText()
                        // Handle WebSocket messages if needed
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

