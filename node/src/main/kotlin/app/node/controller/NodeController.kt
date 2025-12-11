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
        
        // Ask CS Host; if granted immediately, access resource now
        // If queued, will be handled when CSState update arrives
        val granted = runBlocking {
            csInteractionController.requestAccess(config.nodeId, currentRequestId!!)
        }
        if (granted) {
            eventLogger.success("CS Host granted access immediately", 
                mapOf("requestId" to currentRequestId!!))
            // Access default resource as part of CS entry coordination
            runBlocking {
                csInteractionController.accessResource("counter", config.nodeId, currentRequestId!!)
            }
            // Will enter CS when Ricart-Agrawala algorithm allows (after receiving all replies)
        } else {
            eventLogger.warning("CS Host queued request", 
                mapOf("requestId" to currentRequestId!!, "queuePosition" to "pending"))
        }
        // If not granted (queued), wait for CSState update to detect when granted
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
        
        // Log queue position changes
        val wasInQueue = previousState?.queue?.contains(config.nodeId) ?: false
        val isInQueue = state.queue.contains(config.nodeId)
        val queuePosition = if (isInQueue) {
            state.queue.indexOf(config.nodeId) + 1
        } else null
        
        if (!wasInQueue && isInQueue) {
            eventLogger.info("Added to CS queue", mapOf("position" to queuePosition.toString()))
        } else if (wasInQueue && !isInQueue && queuePosition != null) {
            eventLogger.info("Removed from CS queue", mapOf("previousPosition" to queuePosition.toString()))
        } else if (isInQueue && queuePosition != null) {
            eventLogger.info("Queue position updated", mapOf("position" to queuePosition.toString()))
        }
        
        // Check if this node was just granted access from queue
        // This happens when CSHost grants access to next node in queue after previous holder releases
        if (state.currentHolder == config.nodeId && 
            !inCriticalSection && 
            currentRequestId != null &&
            (previousState == null || previousState.currentHolder != config.nodeId)) {
            // Node was just granted access from queue
            eventLogger.success("CS Host granted access from queue", 
                mapOf("requestId" to currentRequestId!!))
            // Access resource and enter CS
            runBlocking {
                csInteractionController.accessResource("counter", config.nodeId, currentRequestId!!)
            }
            // Note: inCriticalSection will be set to true by onEnterCriticalSection()
            // which is called by RicartAgrawala when all replies are received
            // But if we already have all replies (deferredReplies empty), we can enter now
            val raState = ricartAgrawala.getState()
            if (raState.deferredReplies.isEmpty() && raState.requesting) {
                // We have all replies, can enter CS
                onEnterCriticalSection()
            }
        }
        
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
        // Only enter CS if CSHost has granted access
        // This ensures we don't enter CS before being granted from queue
        val state = csHostState
        if (state?.currentHolder == config.nodeId) {
            inCriticalSection = true
            eventLogger.success("Entered critical section", 
                mapOf("requestId" to (currentRequestId ?: "unknown"), "clock" to ricartAgrawala.getClock().toString()))
        }
        // If CSHost hasn't granted yet, we'll enter when updateCsHostState detects grant
    }
    
    fun onExitCriticalSection() {
        if (inCriticalSection) {
            eventLogger.info("Exited critical section", 
                mapOf("requestId" to (currentRequestId ?: "unknown")))
        }
        inCriticalSection = false
    }
    
    fun onNodeDiscovered(nodeConfig: NodeConfig) {
        connectedNodes.add(nodeConfig.nodeId)
        eventLogger.info("Node discovered", 
            mapOf("nodeId" to nodeConfig.nodeId, "host" to nodeConfig.host, "port" to nodeConfig.port.toString()))
    }
    
    fun onNodeLost(nodeId: String) {
        connectedNodes.remove(nodeId)
        eventLogger.warning("Node lost", mapOf("nodeId" to nodeId))
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



