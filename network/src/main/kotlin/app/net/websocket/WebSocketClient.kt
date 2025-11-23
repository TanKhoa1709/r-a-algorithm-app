package app.net.websocket

import app.models.NodeConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WebSocket client for connecting to other nodes
 */
class WebSocketClient {
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }
    
    suspend fun connect(
        nodeConfig: NodeConfig,
        onMessage: (String) -> Unit
    ): WebSocketSession {
        return client.webSocketSession(
            host = nodeConfig.host,
            port = nodeConfig.port,
            path = "/ws?nodeId=${nodeConfig.nodeId}"
        ).also { session ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (frame in session.incoming) {
                        if (frame is Frame.Text) {
                            onMessage(frame.readText())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun close() {
        client.close()
    }
}

