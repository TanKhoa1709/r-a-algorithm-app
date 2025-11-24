package app.core

import app.proto.RAMessage
import java.util.concurrent.ConcurrentHashMap

/**
 * State management for Ricart-Agrawala algorithm
 */
data class RAState(
    val nodeId: String,
    val lamportClock: LamportClock = LamportClock(),
    var requesting: Boolean = false,
    var requestTimestamp: Long = 0,
    var requestId: String? = null,
    val deferredReplies: MutableSet<String> = ConcurrentHashMap.newKeySet(),
    val pendingRequests: MutableMap<String, RAMessage> = ConcurrentHashMap()
) {
    fun isInCriticalSection(): Boolean = requesting && deferredReplies.isEmpty()
    
    fun hasPendingRequest(): Boolean = requestId != null && requesting
    
    fun addDeferredReply(nodeId: String) {
        deferredReplies.add(nodeId)
    }
    
    fun removeDeferredReply(nodeId: String) {
        deferredReplies.remove(nodeId)
    }
    
    fun clearDeferredReplies() {
        deferredReplies.clear()
    }
    
    fun addPendingRequest(request: RAMessage) {
        pendingRequests[request.nodeId] = request
    }
    
    fun removePendingRequest(nodeId: String) {
        pendingRequests.remove(nodeId)
    }
}

