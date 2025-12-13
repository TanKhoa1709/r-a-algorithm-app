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
    val pendingRequests: MutableMap<String, RAMessage> = ConcurrentHashMap(),
    var hasEnteredCS: Boolean = false,  // Track xem đã vào CS chưa để tránh enter nhiều lần
    val repliedRequests: MutableSet<String> = ConcurrentHashMap.newKeySet()  // Track các request đã reply (format: "nodeId:requestId")
) {
    fun isInCriticalSection(): Boolean = requesting && deferredReplies.isEmpty() && hasEnteredCS
    
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

