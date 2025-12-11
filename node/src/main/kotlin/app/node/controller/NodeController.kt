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
    
    /**
     * Request to enter critical section
     */
    fun requestCriticalSection(): String {
        if (inCriticalSection) {
            throw IllegalStateException("Already in critical section")
        }
        if (currentRequestId != null) {
            throw IllegalStateException("Already have a pending request")
        }
        currentRequestId = ricartAgrawala.requestCriticalSection()
        // Ask CS Host; if granted immediately, access resource now
        // If queued, will be handled when CSState update arrives
        val granted = runBlocking {
            csInteractionController.requestAccess(config.nodeId, currentRequestId!!)
        }
        if (granted) {
            // Access default resource as part of CS entry coordination
            runBlocking {
                csInteractionController.accessResource("counter", config.nodeId, currentRequestId!!)
            }
            // Will enter CS when Ricart-Agrawala algorithm allows (after receiving all replies)
        }
        // If not granted (queued), wait for CSState update to detect when granted
        return currentRequestId!!
    }
    
    /**
     * Release critical section
     */
    fun releaseCriticalSection() {
        if (!inCriticalSection || currentRequestId == null) {
            throw IllegalStateException("Not in critical section")
        }
        // Release CS host first, then notify peers
        runBlocking {
            csInteractionController.releaseResource("counter", config.nodeId, currentRequestId!!)
            csInteractionController.releaseAccess(config.nodeId, currentRequestId!!)
        }
        ricartAgrawala.releaseCriticalSection()
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
        
        // Check if this node was just granted access from queue
        // This happens when CSHost grants access to next node in queue after previous holder releases
        if (state.currentHolder == config.nodeId && 
            !inCriticalSection && 
            currentRequestId != null &&
            (previousState == null || previousState.currentHolder != config.nodeId)) {
            // Node was just granted access from queue
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
        }
        // If CSHost hasn't granted yet, we'll enter when updateCsHostState detects grant
    }
    
    fun onExitCriticalSection() {
        inCriticalSection = false
    }
    
    fun onNodeDiscovered(nodeConfig: NodeConfig) {
        connectedNodes.add(nodeConfig.nodeId)
    }
    
    fun onNodeLost(nodeId: String) {
        connectedNodes.remove(nodeId)
    }
}



