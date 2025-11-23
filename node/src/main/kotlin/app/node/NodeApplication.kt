package app.node

import app.core.RicartAgrawala
import app.models.NodeConfig as SharedNodeConfig
import app.net.discovery.NodeAnnouncer
import app.net.discovery.ServiceDiscovery
import app.net.discovery.DiscoveryConfig
import app.net.websocket.ConnectionManager
import app.net.websocket.WebSocketServer
import app.node.controller.NodeController
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Main orchestrator for the node application
 */
class NodeApplication(private val config: NodeConfig) {
    private lateinit var ricartAgrawala: RicartAgrawala
    private lateinit var connectionManager: ConnectionManager
    private lateinit var webSocketServer: WebSocketServer
    private lateinit var nodeAnnouncer: NodeAnnouncer
    private lateinit var serviceDiscovery: ServiceDiscovery
    private lateinit var controller: NodeController
    
    fun start() {
        val sharedConfig = config.sharedConfig
        
        // Initialize network components
        connectionManager = ConnectionManager { nodeId, message ->
            handleMessage(nodeId, message)
        }
        
        webSocketServer = WebSocketServer(sharedConfig.port) { nodeId, message ->
            handleMessage(nodeId, message)
        }
        webSocketServer.start()
        
        // Initialize Ricart-Agrawala algorithm
        ricartAgrawala = RicartAgrawala(
            nodeId = sharedConfig.nodeId,
            onSendRequest = { message ->
                broadcastMessage(Json.encodeToString(message))
            },
            onSendReply = { message ->
                sendToNode(message.nodeId, Json.encodeToString(message))
            },
            onSendRelease = { message ->
                broadcastMessage(Json.encodeToString(message))
            },
            onEnterCS = {
                controller.onEnterCriticalSection()
            },
            onExitCS = {
                controller.onExitCriticalSection()
            }
        )
        
        // Initialize service discovery
        val discoveryConfig = DiscoveryConfig()
        nodeAnnouncer = NodeAnnouncer(discoveryConfig, sharedConfig)
        nodeAnnouncer.start()
        
        serviceDiscovery = ServiceDiscovery(
            discoveryConfig,
            onNodeDiscovered = { nodeConfig ->
                ricartAgrawala.registerNode(nodeConfig.nodeId)
                runBlocking {
                    connectionManager.connectToNode(nodeConfig)
                }
                controller.onNodeDiscovered(nodeConfig)
            },
            onNodeLost = { nodeId ->
                ricartAgrawala.unregisterNode(nodeId)
                runBlocking {
                    connectionManager.disconnectFromNode(nodeId)
                }
                controller.onNodeLost(nodeId)
            }
        )
        serviceDiscovery.start()
        
        // Initialize controller
        controller = NodeController(
            ricartAgrawala,
            connectionManager,
            sharedConfig
        )
    }
    
    fun stop() {
        nodeAnnouncer.stop()
        serviceDiscovery.stop()
        webSocketServer.stop()
        connectionManager.close()
    }
    
    fun getController(): NodeController = controller
    
    private fun handleMessage(nodeId: String, message: String) {
        try {
            val protocol = Json.decodeFromString<app.proto.CSProtocol>(message)
            when (protocol.type) {
                "REQUEST" -> {
                    val request = Json.decodeFromString<app.proto.RequestMessage>(protocol.payload)
                    ricartAgrawala.handleRequest(request)
                }
                "REPLY" -> {
                    val reply = Json.decodeFromString<app.proto.ReplyMessage>(protocol.payload)
                    ricartAgrawala.handleReply(reply)
                }
                "RELEASE" -> {
                    val release = Json.decodeFromString<app.proto.ReleaseMessage>(protocol.payload)
                    ricartAgrawala.handleRelease(release)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun broadcastMessage(message: String) {
        runBlocking {
            connectionManager.broadcast(message)
        }
    }
    
    private fun sendToNode(nodeId: String, message: String) {
        runBlocking {
            connectionManager.sendToNode(nodeId, message)
        }
    }
}

