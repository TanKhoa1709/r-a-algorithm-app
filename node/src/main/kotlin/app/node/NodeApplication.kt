package app.node

import app.core.RicartAgrawala
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

        // Initialize controller first with a temporary ricartAgrawala
        // We'll replace it after creating the real one
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

        // Now create the real ricartAgrawala with proper callbacks
        ricartAgrawala = RicartAgrawala(
            nodeId = sharedConfig.nodeId,
            onSendRequest = { message ->
                if (::controller.isInitialized) {
                    controller.logSendingRequest(message.requestId, message.timestamp)
                }
                val protocol = app.proto.CSProtocol.fromMessage(message)
                broadcastMessage(Json.encodeToString(protocol))
            },
            onSendReply = { message, targetNodeId ->
                if (::controller.isInitialized) {
                    controller.logSendingReply(targetNodeId, message.requestId, message.timestamp)
                }
                val protocol = app.proto.CSProtocol.fromMessage(message)
                // Send reply to the original requester (targetNodeId), not the sender
                sendToNode(targetNodeId, Json.encodeToString(protocol))
            },
            onSendRelease = { message ->
                if (::controller.isInitialized) {
                    controller.logSendingRelease(message.requestId, message.timestamp)
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

        // Recreate controller with the real ricartAgrawala
        controller = NodeController(
            ricartAgrawala,
            connectionManager,
            csInteractionController,
            sharedConfig
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

        // Start CS Host websocket subscription and seed initial state
        csHostWebSocketClient = CSHostWebSocketClient(
            sharedConfig.csHostUrl,
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
                controller.logIncomingRequest(ramessage.nodeId, ramessage.requestId, ramessage.timestamp)
                ricartAgrawala.handleRequest(ramessage)
            },
            onReply = { ramessage ->
                controller.logIncomingReply(ramessage.nodeId, ramessage.requestId, ramessage.timestamp)
                ricartAgrawala.handleReply(ramessage)
            },
            onRelease = { ramessage ->
                controller.logIncomingRelease(ramessage.nodeId, ramessage.requestId, ramessage.timestamp)
                ricartAgrawala.handleRelease(ramessage)
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

    fun updateCsHostUrl(newUrl: String) {
        try {
            val file = File(config.configPath)

            // Json dùng để đọc & ghi file config
            val json = Json {
                prettyPrint = true
                encodeDefaults = true
            }

            val rawConfig = if (file.exists()) {
                json.decodeFromString<app.models.NodeConfig>(file.readText())
            } else {
                // fallback: nếu file bị mất thì lấy config hiện tại
                config.sharedConfig
            }

            val updated = rawConfig.copy(csHostUrl = newUrl)

            // Ghi lại ra file
            val newText = json.encodeToString(app.models.NodeConfig.serializer(), updated)
            file.writeText(newText)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
