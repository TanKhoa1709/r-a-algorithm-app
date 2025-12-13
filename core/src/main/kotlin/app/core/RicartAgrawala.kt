package app.core

import app.proto.MsgType
import app.proto.RAMessage
import java.util.UUID

/**
 * Main Ricart-Agrawala algorithm implementation
 */
class RicartAgrawala(
    private val nodeId: String,
    private val onSendRequest: (RAMessage) -> Unit,
    private val onSendReply: (RAMessage, String) -> Unit,
    private val onSendRelease: (RAMessage) -> Unit,
    private val onEnterCS: () -> Unit,
    private val onExitCS: () -> Unit
) {
    private val state = RAState(nodeId)
    private val allNodes = mutableSetOf<String>()
    
    /**
     * Register a node in the system
     */
    fun registerNode(nodeId: String) {
        allNodes.add(nodeId)
    }
    
    /**
     * Unregister a node from the system
     */
    fun unregisterNode(nodeId: String) {
        allNodes.remove(nodeId)
        state.removePendingRequest(nodeId)
        state.removeDeferredReply(nodeId)
    }
    
    /**
     * Request to enter critical section
     */
    fun requestCriticalSection(): String {
        if (state.requesting) {
            throw IllegalStateException("Already requesting critical section")
        }
        
        val requestId = UUID.randomUUID().toString()
        val timestamp = state.lamportClock.tick()
        
        state.requesting = true
        state.requestTimestamp = timestamp
        state.requestId = requestId
        state.hasEnteredCS = false
        state.repliedRequests.clear()
        
        val request = RAMessage(
            type = MsgType.REQUEST,
            timestamp = timestamp,
            nodeId = nodeId,
            requestId = requestId
        )
        
        val otherNodes = allNodes.filter { it != nodeId }
        state.deferredReplies.clear()
        state.deferredReplies.addAll(otherNodes)
        
        allNodes.forEach { targetNode ->
            if (targetNode != nodeId) {
                onSendRequest(request)
            }
        }
        
        checkAndEnterCS()
        
        return requestId
    }
    
    /**
     * Handle incoming request message
     */
    fun handleRequest(request: RAMessage) {
        require(request.type == MsgType.REQUEST) { "Expected REQUEST message type" }
        state.lamportClock.receive(request.timestamp)
        
        if (request.nodeId == nodeId) {
            return
        }
        
        val requestKey = "${request.nodeId}:${request.requestId}"
        val alreadyReplied = state.repliedRequests.contains(requestKey)
        val alreadyDeferred = state.pendingRequests.containsKey(request.nodeId) && 
                              state.pendingRequests[request.nodeId]?.requestId == request.requestId
        
        if (alreadyReplied || alreadyDeferred) {
            return
        }
        
        val shouldReply = when {
            !state.requesting -> true
            state.requestTimestamp > request.timestamp -> true
            state.requestTimestamp == request.timestamp && nodeId > request.nodeId -> true
            else -> false
        }
        
        if (shouldReply) {
            val reply = RAMessage(
                type = MsgType.REPLY,
                timestamp = state.lamportClock.tick(),
                nodeId = nodeId,
                requestId = request.requestId
            )
            // Send reply directly to the requester
            onSendReply(reply, request.nodeId)
            // Mark as replied to avoid duplicate replies
            state.repliedRequests.add(requestKey)
        } else {
            // Defer reply
            state.addPendingRequest(request)
        }
    }
    
    /**
     * Handle incoming reply message
     */
    fun handleReply(reply: RAMessage) {
        require(reply.type == MsgType.REPLY) { "Expected REPLY message type" }
        state.lamportClock.receive(reply.timestamp)
        
        if (reply.nodeId == nodeId) {
            return // Ignore own replies
        }
        
        // Chỉ xử lý reply nếu:
        // 1. Đang request CS
        // 2. Reply match với requestId hiện tại
        // 3. Node này vẫn đang trong deferredReplies (chưa được xử lý)
        if (state.requesting && 
            state.requestId == reply.requestId && 
            state.deferredReplies.contains(reply.nodeId)) {
            // Remove this node from the set of nodes we're waiting for
            state.removeDeferredReply(reply.nodeId)
            checkAndEnterCS()
        }
    }
    
    /**
     * Release critical section
     */
    fun releaseCriticalSection() {
        if (!state.requesting || state.requestId == null) {
            throw IllegalStateException("Not in critical section")
        }
        
        val release = RAMessage(
            type = MsgType.RELEASE,
            timestamp = state.lamportClock.tick(),
            nodeId = nodeId,
            requestId = state.requestId!!
        )
        
        // Send release to all other nodes
        allNodes.forEach { targetNode ->
            if (targetNode != nodeId) {
                onSendRelease(release)
            }
        }
        
        // Process deferred requests
        val deferredRequests = state.pendingRequests.values.toList()
        state.pendingRequests.clear()
        
        deferredRequests.forEach { request ->
            val requestKey = "${request.nodeId}:${request.requestId}"
            // Chỉ gửi reply nếu chưa reply trước đó (tránh duplicate)
            if (!state.repliedRequests.contains(requestKey)) {
                val reply = RAMessage(
                    type = MsgType.REPLY,
                    timestamp = state.lamportClock.tick(),
                    nodeId = nodeId,
                    requestId = request.requestId
                )
                onSendReply(reply, request.nodeId)
                state.repliedRequests.add(requestKey)
            }
        }
        
        // Reset state
        state.requesting = false
        state.requestTimestamp = 0
        state.requestId = null
        state.hasEnteredCS = false
        state.clearDeferredReplies()
        
        onExitCS()
    }
    
    /**
     * Handle incoming release message
     */
    fun handleRelease(release: RAMessage) {
        require(release.type == MsgType.RELEASE) { "Expected RELEASE message type" }
        state.lamportClock.receive(release.timestamp)
        // Release messages are informational, no action needed
    }
    
    /**
     * Check if we can enter critical section
     */
    private fun checkAndEnterCS() {
        // We can enter CS if we're requesting and have received all replies
        // (deferredReplies contains nodes we're waiting for, so empty means we have all replies)
        // VÀ chưa vào CS (tránh enter nhiều lần)
        if (state.requesting && state.deferredReplies.isEmpty() && !state.hasEnteredCS) {
            // We have all replies, can enter CS
            state.hasEnteredCS = true
            onEnterCS()
        }
    }
    
    /**
     * Get current state
     */
    fun getState(): RAState = state.copy()
    
    /**
     * Get current clock value
     */
    fun getClock(): Long = state.lamportClock.getTime()
}

