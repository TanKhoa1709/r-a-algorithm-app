package app.net.websocket

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch

/**
 * WebSocket server for node communication
 */
class WebSocketServer(
    private val port: Int,
    private val onMessage: (String, String) -> Unit
) {
    private var server: ApplicationEngine? = null
    
    fun start() {
        server = embeddedServer(Netty, port = port) {
            install(io.ktor.server.plugins.websocket.WebSockets)
            routing {
                webSocket("/ws") {
                    val nodeId = call.request.queryParameters["nodeId"] ?: "unknown"
                    
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val message = frame.readText()
                                onMessage(nodeId, message)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }.start(wait = false)
    }
    
    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}

