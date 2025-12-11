package app.node

import app.core.RicartAgrawala
import app.models.NodeConfig as SharedNodeConfig
import app.net.discovery.NodeAnnouncer
import app.net.discovery.ServiceDiscovery
import app.net.discovery.DiscoveryConfig
import app.net.websocket.ConnectionManager
import app.net.websocket.WebSocketServer
import app.node.controller.CSInteractionController
import app.node.controller.MessageHandler
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
    private lateinit var csInteractionController: CSInteractionController
    private var csHostWebSocketClient: CSHostWebSocketClient? = null
    private lateinit var controller: NodeController
    private val messageHandler = MessageHandler()
    
    fun start() {
        val sharedConfig = config.sharedConfig
        
        csInteractionController = CSInteractionController(sharedConfig.csHostUrl)
        // Initialize network components
        connectionManager = ConnectionManager { _, message ->
            handleMessage(message)
        }
        
        webSocketServer = WebSocketServer(sharedConfig.port) { _, message ->
            handleMessage(message)
        }
        webSocketServer.start()
        
        // Initialize Ricart-Agrawala algorithm
        ricartAgrawala = RicartAgrawala(
            nodeId = sharedConfig.nodeId,
            onSendRequest = { message ->
                val protocol = app.proto.CSProtocol.fromMessage(message)
                broadcastMessage(Json.encodeToString(protocol))
            },
            onSendReply = { message, targetNodeId ->
                val protocol = app.proto.CSProtocol.fromMessage(message)
                // Send reply to the original requester (targetNodeId), not the sender
                sendToNode(targetNodeId, Json.encodeToString(protocol))
            },
            onSendRelease = { message ->
                val protocol = app.proto.CSProtocol.fromMessage(message)
                broadcastMessage(Json.encodeToString(protocol))
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
            csInteractionController,
            sharedConfig
        )

        // Start CS Host websocket subscription and seed initial state
        csHostWebSocketClient = CSHostWebSocketClient(sharedConfig.csHostUrl,
            onState = { state -> controller.updateCsHostState(state) },
            onError = { ex -> println("CS Host WS error: ${ex.message}") }
        ).also { it.start() }
        runCatching {
            val initial = runBlocking { csInteractionController.getState() }
            controller.updateCsHostState(initial)
        }
    }
    
    fun stop() {
        nodeAnnouncer.stop()
        serviceDiscovery.stop()
        webSocketServer.stop()
        csInteractionController.close()
        runBlocking {
            connectionManager.close()
            csHostWebSocketClient?.stop()
        }
    }
    
    fun getController(): NodeController = controller
    
    private fun handleMessage(message: String) {
        messageHandler.handleMessage(
            message,
            onRequest = { RAMessage -> ricartAgrawala.handleRequest(RAMessage) },
            onReply = { RAMessage -> ricartAgrawala.handleReply(RAMessage) },
            onRelease = { RAMessage -> ricartAgrawala.handleRelease(RAMessage) }
        )
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

