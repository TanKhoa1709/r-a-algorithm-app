package app.node.controller

import app.core.RicartAgrawala
import app.models.CSState
import app.models.NodeConfig
import app.net.websocket.ConnectionManager
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

/**
 * Controller for node business logic
 */
class NodeController(
    private val ricartAgrawala: RicartAgrawala,
    private val connectionManager: ConnectionManager,
    private val csInteractionController: CSInteractionController,
    private val config: NodeConfig
) {
    private var currentRequestId: String? = null
    private var inCriticalSection = false
    private val connectedNodes = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var csHostState: CSState? = null
    val eventLogger = EventLogger(maxEntries = 200)
    
    /**
     * Request to enter critical section
     */
    fun requestCriticalSection(): String {
        if (inCriticalSection) {
            eventLogger.error("Cannot request CS: Already in critical section")
            throw IllegalStateException("Already in critical section")
        }
        if (currentRequestId != null) {
            eventLogger.warning("Cannot request CS: Already have a pending request", 
                mapOf("requestId" to currentRequestId!!))
            throw IllegalStateException("Already have a pending request")
        }
        currentRequestId = ricartAgrawala.requestCriticalSection()
        eventLogger.info("Requested critical section", 
            mapOf("requestId" to currentRequestId!!, "timestamp" to ricartAgrawala.getClock().toString()))
        
        // Note: KHÔNG request CS Host ở đây
        // CS Host chỉ được gọi SAU KHI đã vào CS (khi Ricart-Agrawala cho phép)
        // Ricart-Agrawala algorithm quyết định thứ tự vào CS (distributed)
        return currentRequestId!!
    }
    
    /**
     * Release critical section
     */
    fun releaseCriticalSection() {
        if (!inCriticalSection || currentRequestId == null) {
            eventLogger.error("Cannot release CS: Not in critical section")
            throw IllegalStateException("Not in critical section")
        }
        val requestId = currentRequestId!!
        eventLogger.info("Releasing critical section", mapOf("requestId" to requestId))
        
        // Release CS host first, then notify peers
        runBlocking {
            csInteractionController.releaseResource("counter", config.nodeId, requestId)
            csInteractionController.releaseAccess(config.nodeId, requestId)
        }
        ricartAgrawala.releaseCriticalSection()
        eventLogger.success("Critical section released", mapOf("requestId" to requestId))
        currentRequestId = null
    }
    
    /**
     * Get current state
     */
    fun getState() = ricartAgrawala.getState()
    
    /**
     * Get clock value
     */
    fun getClock() = ricartAgrawala.getClock()
    
    /**
     * Check if in critical section
     */
    fun isInCriticalSection() = inCriticalSection
    
    /**
     * Check if node has a pending request (in queue waiting for CS)
     */
    fun hasPendingRequest(): Boolean {
        return currentRequestId != null && !inCriticalSection
    }
    
    /**
     * Get connected nodes
     */
    fun getConnectedNodes(): Set<String> = connectedNodes

    fun updateCsHostState(state: CSState) {
        val previousState = csHostState
        csHostState = state
        
        // Note: KHÔNG có queue nữa - CS Host không phải coordinator
        // Ricart-Agrawala algorithm quyết định thứ tự vào CS (distributed)
        
        // Note: KHÔNG có logic "granted from queue" nữa
        // CS Host không có queue - Ricart-Agrawala algorithm quyết định thứ tự vào CS
        // Node vào CS khi nhận đủ replies từ Ricart-Agrawala, sau đó mới request resource từ CS Host
        
        // Check if this node lost access (shouldn't happen, but handle gracefully)
        if (previousState?.currentHolder == config.nodeId && 
            state.currentHolder != config.nodeId && 
            inCriticalSection) {
            // Lost access unexpectedly, exit CS
            eventLogger.error("Lost CS access unexpectedly", 
                mapOf("previousHolder" to config.nodeId, "newHolder" to (state.currentHolder ?: "none")))
            onExitCriticalSection()
        }
    }

    fun getCsHostState(): CSState? = csHostState
    
    suspend fun refreshCsHostState() {
        runCatching {
            csInteractionController.getState()
        }.onSuccess { state ->
            csHostState = state
        }
    }
    
    fun onEnterCriticalSection() {
        // Chỉ xử lý nếu chưa vào CS (tránh enter nhiều lần)
        if (inCriticalSection) {
            return // Đã vào CS rồi, không xử lý lại
        }
        
        // Enter CS khi Ricart-Agrawala algorithm cho phép (đã nhận đủ replies)
        inCriticalSection = true
        eventLogger.success("Entered critical section", 
            mapOf("requestId" to (currentRequestId ?: "unknown"), "clock" to ricartAgrawala.getClock().toString()))
        
        // SAU KHI vào CS, request access resource từ CS Host
        // CS Host chỉ kiểm tra resource availability, không phải coordinator
        if (currentRequestId != null) {
            runBlocking {
                val granted = csInteractionController.requestAccess(config.nodeId, currentRequestId!!)
                if (granted) {
                    eventLogger.success("CS Host resource available", 
                        mapOf("requestId" to currentRequestId!!))
                    // Access default resource
                    csInteractionController.accessResource("counter", config.nodeId, currentRequestId!!)
                } else {
                    eventLogger.warning("CS Host resource busy - should not happen if Ricart-Agrawala correct", 
                        mapOf("requestId" to currentRequestId!!))
                    // This should not happen if Ricart-Agrawala is working correctly
                    // But we still entered CS, so log warning
                }
            }
        }
    }
    
    fun onExitCriticalSection() {
        if (inCriticalSection) {
            eventLogger.info("Exited critical section", 
                mapOf("requestId" to (currentRequestId ?: "unknown")))
        }
        inCriticalSection = false
    }
    
    fun onNodeDiscovered(nodeConfig: NodeConfig) {
        val added = connectedNodes.add(nodeConfig.nodeId)
        // Log chỉ khi trạng thái thực sự thay đổi (node mới được thêm)
        if (added) {
            eventLogger.info(
                "Node discovered",
                mapOf("nodeId" to nodeConfig.nodeId, "host" to nodeConfig.host, "port" to nodeConfig.port.toString())
            )
        }
    }
    
    fun onNodeLost(nodeId: String) {
        val removed = connectedNodes.remove(nodeId)
        // Log chỉ khi trạng thái thực sự thay đổi (node vừa bị mất)
        if (removed) {
            eventLogger.warning("Node lost", mapOf("nodeId" to nodeId))
        }
    }
    
    /**
     * Log incoming REQUEST message
     */
    fun logIncomingRequest(fromNodeId: String, requestId: String, timestamp: Long) {
        eventLogger.requestReceived("Received REQUEST", 
            mapOf("from" to fromNodeId, "requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    /**
     * Log incoming REPLY message
     */
    fun logIncomingReply(fromNodeId: String, requestId: String, timestamp: Long) {
        eventLogger.replyReceived("Received REPLY", 
            mapOf("from" to fromNodeId, "requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    /**
     * Log incoming RELEASE message
     */
    fun logIncomingRelease(fromNodeId: String, requestId: String, timestamp: Long) {
        eventLogger.releaseReceived("Received RELEASE", 
            mapOf("from" to fromNodeId, "requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    /**
     * Log sending REQUEST message
     */
    fun logSendingRequest(requestId: String, timestamp: Long) {
        eventLogger.requestSent("Sent REQUEST to all nodes", 
            mapOf("requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    /**
     * Log sending REPLY message
     */
    fun logSendingReply(toNodeId: String, requestId: String, timestamp: Long) {
        eventLogger.replySent("Sent REPLY", 
            mapOf("to" to toNodeId, "requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    /**
     * Log sending RELEASE message
     */
    fun logSendingRelease(requestId: String, timestamp: Long) {
        eventLogger.releaseSent("Sent RELEASE to all nodes", 
            mapOf("requestId" to requestId, "timestamp" to timestamp.toString()))
    }
}



