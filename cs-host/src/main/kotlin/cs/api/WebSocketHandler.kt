package cs.api

import cs.CSHost
import cs.monitor.NodeBroadcaster
import cs.monitor.VisualizerBroadcaster
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.webSocket
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
            NodeBroadcaster.register(this)
            try {
                val state = csHost.getState()
                send(Frame.Text(Json.encodeToString(state)))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        frame.readText()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                NodeBroadcaster.unregister(this)
            }
        }
        webSocket("/visualizer") {
            VisualizerBroadcaster.register(this)
            try {
                val snapshot = csHost.buildSnapshot()
                send(Frame.Text(Json.encodeToString(snapshot)))
                
                for (frame in incoming) {
                    // Keep connection alive
                }
            } finally {
                VisualizerBroadcaster.unregister(this)
            }
        }

    }
}

