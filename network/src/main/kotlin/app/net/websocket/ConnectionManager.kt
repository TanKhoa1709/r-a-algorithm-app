package app.net.websocket

import app.models.NodeConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages WebSocket connections to other nodes
 */
class ConnectionManager(
    private val onMessage: (String, String) -> Unit
) {
    private val connections = ConcurrentHashMap<String, WebSocketSession>()
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    /**
     * Connect to a node
     */
    suspend fun connectToNode(nodeConfig: NodeConfig) {
        try {
            val session = client.webSocketSession(
                host = nodeConfig.host,
                port = nodeConfig.port,
                path = "/ws"
            )
            connections[nodeConfig.nodeId] = session

            // Start receiving messages
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (frame in session.incoming) {
                        if (frame is Frame.Text) {
                            val message = frame.readText()
                            onMessage(nodeConfig.nodeId, message)
                        }
                    }
                } catch (e: Exception) {
                    connections.remove(nodeConfig.nodeId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Send message to a node
     */
    suspend fun sendToNode(nodeId: String, message: String) {
        val session = connections[nodeId]
        if (session != null && session.isActive) {
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                connections.remove(nodeId)
            }
        }
    }

    /**
     * Broadcast message to all connected nodes
     */
    suspend fun broadcast(message: String) {
        connections.values.forEach { session ->
            if (session.isActive) {
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    // Connection lost, will be cleaned up
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Disconnect from a node
     */
    suspend fun disconnectFromNode(nodeId: String) {
        connections[nodeId]?.close()
        connections.remove(nodeId)
    }

    /**
     * Close all connections
     */
    suspend fun close() {
        connections.values.forEach { it.close() }
        connections.clear()
        client.close()
    }

    fun isConnected(nodeId: String): Boolean = connections.containsKey(nodeId) && connections[nodeId]?.isActive == true
}

