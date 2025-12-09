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
        currentRequestId = ricartAgrawala.requestCriticalSection()
        // Ask CS Host immediately; if denied, abort the request
        val granted = runBlocking {
            csInteractionController.requestAccess(config.nodeId, currentRequestId!!)
        }
        if (!granted) {
            ricartAgrawala.releaseCriticalSection()
            currentRequestId = null
            throw IllegalStateException("CS Host denied access")
        }
        // Access default resource as part of CS entry coordination
        runBlocking {
            csInteractionController.accessResource("counter", config.nodeId, currentRequestId!!)
        }
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
     * Get connected nodes
     */
    fun getConnectedNodes(): Set<String> = connectedNodes

    fun updateCsHostState(state: CSState) {
        csHostState = state
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
        // At this point CS Host already granted in requestCriticalSection
        inCriticalSection = true
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

