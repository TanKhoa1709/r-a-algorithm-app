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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

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
    private lateinit var sharedConfig: SharedNodeConfig
    
    private var lastSentRequestId: String? = null
    private var lastSentReleaseId: String? = null
    private val processedMessages = mutableSetOf<String>()
    
    fun start() {
        sharedConfig = config.sharedConfig
        
        csInteractionController = CSInteractionController(sharedConfig.csHostUrl)
        connectionManager = ConnectionManager { _, message ->
            handleMessage(message)
        }
        
        webSocketServer = WebSocketServer(sharedConfig.port) { _, message ->
            handleMessage(message)
        }
        webSocketServer.start()
        
        val tempRicartAgrawala = RicartAgrawala(
            nodeId = sharedConfig.nodeId,
            onSendRequest = { },
            onSendReply = { _, _ -> },
            onSendRelease = { },
            onEnterCS = { },
            onExitCS = { }
        )
        controller = NodeController(
            tempRicartAgrawala,
            connectionManager,
            csInteractionController,
            sharedConfig
        )
        
        ricartAgrawala = RicartAgrawala(
            nodeId = sharedConfig.nodeId,
            onSendRequest = { message ->
                if (::controller.isInitialized && lastSentRequestId != message.requestId) {
                    controller.logSendingRequest(message.requestId, message.timestamp)
                    lastSentRequestId = message.requestId
                }
                val protocol = app.proto.CSProtocol.fromMessage(message)
                broadcastMessage(Json.encodeToString(protocol))
            },
            onSendReply = { message, targetNodeId ->
                if (::controller.isInitialized) {
                    controller.logSendingReply(targetNodeId, message.requestId, message.timestamp)
                }
                val protocol = app.proto.CSProtocol.fromMessage(message)
                sendToNode(targetNodeId, Json.encodeToString(protocol))
            },
            onSendRelease = { message ->
                if (::controller.isInitialized && lastSentReleaseId != message.requestId) {
                    controller.logSendingRelease(message.requestId, message.timestamp)
                    lastSentReleaseId = message.requestId
                }
                val protocol = app.proto.CSProtocol.fromMessage(message)
                broadcastMessage(Json.encodeToString(protocol))
            },
            onEnterCS = {
                if (::controller.isInitialized) {
                    controller.onEnterCriticalSection()
                }
            },
            onExitCS = {
                if (::controller.isInitialized) {
                    controller.onExitCriticalSection()
                }
            }
        )
        
        controller = NodeController(
            ricartAgrawala,
            connectionManager,
            csInteractionController,
            sharedConfig
        )
        
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
            onRequest = { ramessage -> 
                if (ramessage.nodeId != sharedConfig.nodeId) {
                    val messageKey = "REQUEST:${ramessage.nodeId}:${ramessage.requestId}:${ramessage.timestamp}"
                    if (processedMessages.add(messageKey)) {
                        controller.logIncomingRequest(ramessage.nodeId, ramessage.requestId, ramessage.timestamp)
                        if (processedMessages.size > 1000) {
                            processedMessages.clear()
                        }
                    }
                    ricartAgrawala.handleRequest(ramessage)
                }
            },
            onReply = { ramessage -> 
                if (ramessage.nodeId != sharedConfig.nodeId) {
                    val messageKey = "REPLY:${ramessage.nodeId}:${ramessage.requestId}:${ramessage.timestamp}"
                    if (processedMessages.add(messageKey)) {
                        controller.logIncomingReply(ramessage.nodeId, ramessage.requestId, ramessage.timestamp)
                        if (processedMessages.size > 1000) {
                            processedMessages.clear()
                        }
                    }
                    ricartAgrawala.handleReply(ramessage)
                }
            },
            onRelease = { ramessage -> 
                if (ramessage.nodeId != sharedConfig.nodeId) {
                    val messageKey = "RELEASE:${ramessage.nodeId}:${ramessage.requestId}:${ramessage.timestamp}"
                    if (processedMessages.add(messageKey)) {
                        controller.logIncomingRelease(ramessage.nodeId, ramessage.requestId, ramessage.timestamp)
                        if (processedMessages.size > 1000) {
                            processedMessages.clear()
                        }
                    }
                    ricartAgrawala.handleRelease(ramessage)
                }
            }
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
