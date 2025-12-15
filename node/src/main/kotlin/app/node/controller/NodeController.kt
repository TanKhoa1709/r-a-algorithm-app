package app.node.controller

import app.core.RicartAgrawala
import app.models.CSState
import app.models.NodeConfig as SharedNodeConfig
import app.models.TransactionResult
import app.net.websocket.ConnectionManager
import app.node.NodeConfig
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
    
    private var pendingTransactionType: String? = null
    private var pendingTransactionAmount: Long? = null
    private var transactionResult: kotlinx.coroutines.CompletableDeferred<TransactionResult>? = null
    
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
        
        pendingTransactionType = "WITHDRAW"
        pendingTransactionAmount = amount
        transactionResult = kotlinx.coroutines.CompletableDeferred()
        currentRequestId = ricartAgrawala.requestCriticalSection()
        return transactionResult!!.await()
    }
    
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
        
        pendingTransactionType = "DEPOSIT"
        pendingTransactionAmount = amount
        transactionResult = kotlinx.coroutines.CompletableDeferred()
        currentRequestId = ricartAgrawala.requestCriticalSection()
        return transactionResult!!.await()
    }
    
    fun getState() = ricartAgrawala.getState()
    fun getClock() = ricartAgrawala.getClock()
    fun isInCriticalSection() = inCriticalSection
    
    fun hasPendingRequest(): Boolean {
        return currentRequestId != null && !inCriticalSection
    }
    
    fun getConnectedNodes(): Set<String> = connectedNodes.filter { it != config.sharedConfig.nodeId }.toSet()
    
    fun getNodeHost(): String = config.sharedConfig.host
    
    fun getNodePort(): Int = config.sharedConfig.port

    fun updateCsHostState(state: CSState) {
        val previousState = csHostState
        csHostState = state
        
        if (previousState?.currentHolder == config.sharedConfig.nodeId && 
            state.currentHolder != config.sharedConfig.nodeId && 
            inCriticalSection) {
            onExitCriticalSection()
        }
    }

    suspend fun refreshCsHostState() {
        runCatching {
            csInteractionController.getState()
        }.onSuccess { state ->
            csHostState = state
        }
    }
    
    fun onEnterCriticalSection() {
        if (inCriticalSection) {
            return
        }
        
        inCriticalSection = true
        
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val transactionType = pendingTransactionType
                val amount = pendingTransactionAmount
                val requestId = currentRequestId
                
                if (transactionType != null && amount != null && requestId != null) {
                    eventLogger.success("Entered CS for $transactionType", 
                        mapOf("requestId" to requestId, "amount" to amount.toString(), "clock" to ricartAgrawala.getClock().toString()))

                    var attempt = 0
                    while (true) {
                        attempt++
                        val result = try {
                            when (transactionType) {
                                "WITHDRAW" -> {
                                    eventLogger.info("Executing withdraw", mapOf("amount" to amount.toString(), "attempt" to attempt.toString()))
                                    csInteractionController.withdraw(config.sharedConfig.nodeId, requestId, amount)
                                }
                                "DEPOSIT" -> {
                                    eventLogger.info("Executing deposit", mapOf("amount" to amount.toString(), "attempt" to attempt.toString()))
                                    csInteractionController.deposit(config.sharedConfig.nodeId, requestId, amount)
                                }
                                else -> TransactionResult(success = false, message = "Unknown transaction type", balance = 0L)
                            }
                        } catch (e: Exception) {
                            val errorMsg = e.message ?: "Unknown error"
                            eventLogger.warning("Bank Host unreachable, will retry in 10s",
                                mapOf("type" to transactionType, "amount" to amount.toString(), "error" to errorMsg, "attempt" to attempt.toString()))
                            delay(10_000)
                            continue // always retry on connection/transport errors
                        }
                        
                        if (result.success) {
                            eventLogger.success("Transaction successful", 
                                mapOf("type" to transactionType, "amount" to amount.toString(), "balance" to result.balance.toString(), "attempt" to attempt.toString()))
                            delay(5000)
                            transactionResult?.complete(result)
                            releaseCriticalSection()
                            break
                        } else {
                            if (transactionType == "WITHDRAW") {
                                eventLogger.warning("Withdraw failed: Insufficient balance", 
                                    mapOf("amount" to amount.toString(), "currentBalance" to result.balance.toString(), "message" to result.message))
                            } else {
                                eventLogger.error("Transaction failed", 
                                    mapOf("type" to transactionType, "amount" to amount.toString(), "message" to result.message))
                            }
                            transactionResult?.complete(result)
                            releaseCriticalSection()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                eventLogger.error("Error in transaction", mapOf("error" to errorMsg))
                transactionResult?.complete(TransactionResult(success = false, message = errorMsg, balance = 0L))
                releaseCriticalSection()
            }
        }
    }
    
    private fun releaseCriticalSection() {
        if (!inCriticalSection || currentRequestId == null) {
            return
        }
        val requestId = currentRequestId!!
        eventLogger.info("Releasing critical section", mapOf("requestId" to requestId))
        
        runBlocking {
            csInteractionController.releaseAccess(config.sharedConfig.nodeId, requestId)
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
    
    fun onNodeDiscovered(nodeConfig: SharedNodeConfig) {
        val added = connectedNodes.add(nodeConfig.nodeId)
        if (added) {
            eventLogger.info(
                "Node discovered",
                mapOf("nodeId" to nodeConfig.nodeId, "host" to nodeConfig.host, "port" to nodeConfig.port.toString())
            )
        }
    }
    
    fun onNodeLost(nodeId: String) {
        val removed = connectedNodes.remove(nodeId)
        if (removed) {
            eventLogger.warning("Node lost", mapOf("nodeId" to nodeId))
        }
    }
    
    fun logIncomingRequest(fromNodeId: String, requestId: String, timestamp: Long) {
        eventLogger.requestReceived("Received REQUEST", 
            mapOf("from" to fromNodeId, "requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    fun logIncomingReply(fromNodeId: String, requestId: String, timestamp: Long) {
        eventLogger.replyReceived("Received REPLY", 
            mapOf("from" to fromNodeId, "requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    fun logIncomingRelease(fromNodeId: String, requestId: String, timestamp: Long) {
        eventLogger.releaseReceived("Received RELEASE", 
            mapOf("from" to fromNodeId, "requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    fun logSendingRequest(requestId: String, timestamp: Long) {
        eventLogger.requestSent("Sent REQUEST to all nodes", 
            mapOf("requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    fun logSendingReply(toNodeId: String, requestId: String, timestamp: Long) {
        eventLogger.replySent("Sent REPLY", 
            mapOf("to" to toNodeId, "requestId" to requestId, "timestamp" to timestamp.toString()))
    }
    
    fun logSendingRelease(requestId: String, timestamp: Long) {
        eventLogger.releaseSent("Sent RELEASE to all nodes", 
            mapOf("requestId" to requestId, "timestamp" to timestamp.toString()))
    }
}



