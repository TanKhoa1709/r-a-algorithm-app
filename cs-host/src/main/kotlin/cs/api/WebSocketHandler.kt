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
            // Register session for real-time updates
            NodeBroadcaster.register(this)
            try {
                // Send initial state
                val state = csHost.getState()
                send(Frame.Text(Json.encodeToString(state)))

                // Handle incoming messages (keep connection alive)
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        // Handle WebSocket messages if needed
                        frame.readText() // Consume frame
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                NodeBroadcaster.unregister(this)
            }
        }
        webSocket("/visualizer") {
            // đăng ký session
            VisualizerBroadcaster.register(this)
            try {
                // Gửi snapshot ngay khi visualizer connect để có balance ban đầu
                val snapshot = csHost.buildSnapshot()
                send(Frame.Text(Json.encodeToString(snapshot)))
                
                // nếu visualizer không gửi gì, có thể chỉ cần giữ connection
                // và đọc/ignore frame cho tới khi client đóng
                for (frame in incoming) {
                    // sau này có thể cho phép client gửi command
                }
            } finally {
                VisualizerBroadcaster.unregister(this)
            }
        }

    }
}

