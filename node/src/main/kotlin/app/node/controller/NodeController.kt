package app.node.controller

import app.core.RicartAgrawala
import app.models.CSState
import app.models.NodeConfig
import app.models.TransactionResult
import app.net.websocket.ConnectionManager
import kotlinx.coroutines.*
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
    
    // Store transaction type and amount for when we enter CS
    private var pendingTransactionType: String? = null  // "WITHDRAW" or "DEPOSIT"
    private var pendingTransactionAmount: Long? = null
    private var transactionResult: kotlinx.coroutines.CompletableDeferred<TransactionResult>? = null
    
    /**
     * Withdraw money from bank account
     * Sử dụng Ricart-Agrawala để đảm bảo mutual exclusion
     */
    suspend fun withdraw(amount: Long): TransactionResult {
        if (inCriticalSection) {
            eventLogger.error("Cannot withdraw: Already in critical section")
            throw IllegalStateException("Already in critical section")
        }
        if (currentRequestId != null) {
            eventLogger.warning("Cannot withdraw: Already have a pending request", 
                mapOf("requestId" to currentRequestId!!))
            throw IllegalStateException("Already have a pending request")
        }
        
        // Store transaction info
        pendingTransactionType = "WITHDRAW"
        pendingTransactionAmount = amount
        transactionResult = kotlinx.coroutines.CompletableDeferred()
        
        // Request CS via Ricart-Agrawala (chưa log ở đây, sẽ log khi đã nhận đủ replies và vào CS)
        currentRequestId = ricartAgrawala.requestCriticalSection()
        
        // Wait for transaction to complete (will be completed in onEnterCriticalSection)
        return transactionResult!!.await()
    }
    
    /**
     * Deposit money to bank account
     * Sử dụng Ricart-Agrawala để đảm bảo mutual exclusion
     */
    suspend fun deposit(amount: Long): TransactionResult {
        if (inCriticalSection) {
            eventLogger.error("Cannot deposit: Already in critical section")
            throw IllegalStateException("Already in critical section")
        }
        if (currentRequestId != null) {
            eventLogger.warning("Cannot deposit: Already have a pending request", 
                mapOf("requestId" to currentRequestId!!))
            throw IllegalStateException("Already have a pending request")
        }
        
        // Store transaction info
        pendingTransactionType = "DEPOSIT"
        pendingTransactionAmount = amount
        transactionResult = kotlinx.coroutines.CompletableDeferred()
        
        // Request CS via Ricart-Agrawala (chưa log ở đây, sẽ log khi đã nhận đủ replies và vào CS)
        currentRequestId = ricartAgrawala.requestCriticalSection()
        
        // Wait for transaction to complete (will be completed in onEnterCriticalSection)
        return transactionResult!!.await()
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
            // Lost access unexpectedly, exit CS (không log)
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
        
        // Thực hiện transaction (withdraw/deposit) trong coroutine
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val transactionType = pendingTransactionType
                val amount = pendingTransactionAmount
                val requestId = currentRequestId
                
                if (transactionType != null && amount != null && requestId != null) {
                    // Log khi đã nhận đủ replies và vào CS (đúng logic: chỉ log khi đã vào CS)
                    eventLogger.success("Entered CS for $transactionType", 
                        mapOf("requestId" to requestId, "amount" to amount.toString(), "clock" to ricartAgrawala.getClock().toString()))
                    
                    // Thực hiện transaction trên CS Host
                    val result = when (transactionType) {
                        "WITHDRAW" -> {
                            eventLogger.info("Executing withdraw", mapOf("amount" to amount.toString()))
                            csInteractionController.withdraw(config.nodeId, requestId, amount)
                        }
                        "DEPOSIT" -> {
                            eventLogger.info("Executing deposit", mapOf("amount" to amount.toString()))
                            csInteractionController.deposit(config.nodeId, requestId, amount)
                        }
                        else -> TransactionResult(success = false, message = "Unknown transaction type", balance = 0L)
                    }
                    
                    if (result.success) {
                        eventLogger.success("Transaction successful", 
                            mapOf("type" to transactionType, "amount" to amount.toString(), "balance" to result.balance.toString()))
                    } else {
                        // Transaction failed (có thể là insufficient balance cho withdraw)
                        if (transactionType == "WITHDRAW") {
                            eventLogger.warning("Withdraw failed: Insufficient balance", 
                                mapOf("amount" to amount.toString(), "currentBalance" to result.balance.toString(), "message" to result.message))
                        } else {
                            eventLogger.error("Transaction failed", 
                                mapOf("type" to transactionType, "amount" to amount.toString(), "message" to result.message))
                        }
                    }
                    
                    // Sleep 5 seconds (ngay cả khi transaction fail)
                    delay(5000)
                    
                    // Complete transaction result
                    transactionResult?.complete(result)
                    
                    // Release CS (ngay cả khi transaction fail - node đã vào CS và thử transaction)
                    releaseCriticalSection()
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                eventLogger.error("Error in transaction", mapOf("error" to errorMsg))
                transactionResult?.complete(TransactionResult(success = false, message = errorMsg, balance = 0L))
                // Release CS even on error
                releaseCriticalSection()
            }
        }
    }
    
    /**
     * Release critical section (internal method)
     */
    private fun releaseCriticalSection() {
        if (!inCriticalSection || currentRequestId == null) {
            return
        }
        val requestId = currentRequestId!!
        eventLogger.info("Releasing critical section", mapOf("requestId" to requestId))
        
        runBlocking {
            csInteractionController.releaseAccess(config.nodeId, requestId)
        }
        ricartAgrawala.releaseCriticalSection()
        eventLogger.success("Critical section released", mapOf("requestId" to requestId))
        currentRequestId = null
        pendingTransactionType = null
        pendingTransactionAmount = null
        transactionResult = null
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



