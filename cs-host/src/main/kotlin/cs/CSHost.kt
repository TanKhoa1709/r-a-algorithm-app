package cs

import app.models.CSState
import app.models.CSEntry
import app.models.TransactionResult
import app.models.Violation
import app.models.VisualizerMetricsDto
import app.models.VisualizerNodeDto
import app.models.VisualizerSnapshot
import cs.monitor.AccessMonitor
import cs.monitor.NodeBroadcaster
import cs.monitor.ViolationDetector
import cs.monitor.VisualizerBroadcaster
import cs.resources.ResourceManager
import cs.resources.SharedResource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Bank Host - Resource Manager (NOT a coordinator)
 * 
 * Bank Host manages the shared bank account and other resources, NOT a coordinator for Ricart-Agrawala algorithm.
 * Ricart-Agrawala algorithm runs distributed between branches to decide CS access order.
 * Bank Host only ensures mutual exclusion at the resource access layer.
 */
class CSHost(
    private val config: CSHostConfig,
    private val resourceManager: ResourceManager
) {
    private val currentHolder: AtomicReference<String?> = AtomicReference(null)
    private val accessHistory = ConcurrentLinkedQueue<CSEntry>()
    private val totalAccesses = AtomicLong(0)
    private val violations = ConcurrentLinkedQueue<Violation>()
    private val accessMutex = Mutex()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val accessMonitor = AccessMonitor(config.enableMonitoring)
    private val violationDetector = ViolationDetector(config.enableViolationDetection)

    suspend fun requestAccess(nodeId: String, requestId: String): Boolean {
        val granted = accessMutex.withLock {
            when {
                currentHolder.get() == null -> {
                    grantAccess(nodeId, requestId)
                    true
                }
                currentHolder.get() == nodeId -> {
                    true
                }
                else -> {
                    false
                }
            }
        }

        notifyNodes()
        notifyVisualizer()
        return granted
    }

    private fun grantAccess(nodeId: String, requestId: String) {
        val entryTime = System.currentTimeMillis()
        currentHolder.set(nodeId)
        totalAccesses.incrementAndGet()

        val entry = CSEntry(
            nodeId = nodeId,
            requestId = requestId,
            timestamp = entryTime,
            entryTime = entryTime
        )
        accessHistory.offer(entry)

        accessMonitor.recordAccess(entry)
        violationDetector.checkAccess(entry, getState())
    }

    suspend fun releaseAccess(nodeId: String, requestId: String) {
        accessMutex.withLock {
            if (currentHolder.get() == nodeId) {
                val exitTime = System.currentTimeMillis()
                currentHolder.set(null)

                val entry = accessHistory.lastOrNull { it.nodeId == nodeId && it.requestId == requestId }
                entry?.let {
                    val updated = it.copy(
                        exitTime = exitTime,
                        duration = exitTime - it.entryTime
                    )
                    accessHistory.remove(it)
                    accessHistory.offer(updated)
                }
            }
        }
        notifyNodes()
        notifyVisualizer()
    }

    fun getState(): CSState {
        return CSState(
            isLocked = currentHolder.get() != null,
            currentHolder = currentHolder.get(),
            queue = emptyList(),
            totalAccesses = totalAccesses.get(),
            violations = violations.toList()
        )
    }

    fun getAccessHistory(): List<CSEntry> = accessHistory.toList()
    fun recordViolation(violation: Violation) {
        violations.offer(violation)
    }
    fun getResourceManager(): ResourceManager = resourceManager
    
    suspend fun withdraw(nodeId: String, requestId: String, amount: Long): TransactionResult {
        val bankAccount = resourceManager.getResource("bank-account") as? cs.resources.BankAccountResource
            ?: return TransactionResult(success = false, message = "Bank account resource not found", balance = 0L)
        
        // Kiểm tra node có đang giữ resource không (đã vào CS)
        val granted = accessMutex.withLock {
            if (currentHolder.get() == null || currentHolder.get() == nodeId) {
                if (currentHolder.get() == null) {
                    grantAccess(nodeId, requestId)
                }
                true
            } else {
                false
            }
        }
        
        if (!granted) {
            return TransactionResult(success = false, message = "Resource not available", balance = bankAccount.getBalance())
        }
        
        val currentBalance = bankAccount.getBalance()
        val success = bankAccount.withdraw(amount)
        val newBalance = bankAccount.getBalance()
        
        val transactionEntry = CSEntry(
            nodeId = nodeId,
            requestId = requestId,
            timestamp = System.currentTimeMillis(),
            entryTime = System.currentTimeMillis(),
            exitTime = System.currentTimeMillis(),
            duration = 0,
            transactionType = "WITHDRAW",
            amount = amount,
            balance = newBalance
        )
        accessHistory.offer(transactionEntry)
        
        notifyNodes()
        notifyVisualizer()
        
        return TransactionResult(
            success = success,
            message = if (success) {
                "Withdrew $amount, new balance: $newBalance"
            } else {
                "Insufficient balance. Current balance: $currentBalance, requested: $amount"
            },
            balance = newBalance
        )
    }
    
    suspend fun deposit(nodeId: String, requestId: String, amount: Long): TransactionResult {
        val bankAccount = resourceManager.getResource("bank-account") as? cs.resources.BankAccountResource
            ?: return TransactionResult(success = false, message = "Bank account resource not found", balance = 0L)
        
        // Kiểm tra node có đang giữ resource không (đã vào CS)
        val granted = accessMutex.withLock {
            if (currentHolder.get() == null || currentHolder.get() == nodeId) {
                if (currentHolder.get() == null) {
                    grantAccess(nodeId, requestId)
                }
                true
            } else {
                false
            }
        }
        
        if (!granted) {
            return TransactionResult(success = false, message = "Resource not available", balance = bankAccount.getBalance())
        }
        
        val success = bankAccount.deposit(amount)
        val newBalance = bankAccount.getBalance()
        
        val transactionEntry = CSEntry(
            nodeId = nodeId,
            requestId = requestId,
            timestamp = System.currentTimeMillis(),
            entryTime = System.currentTimeMillis(),
            exitTime = System.currentTimeMillis(),
            duration = 0,
            transactionType = "DEPOSIT",
            amount = amount,
            balance = newBalance
        )
        accessHistory.offer(transactionEntry)
        
        notifyNodes()
        notifyVisualizer()
        
        return TransactionResult(
            success = success,
            message = "Deposited $amount, new balance: $newBalance",
            balance = newBalance
        )
    }

    fun buildSnapshot(): VisualizerSnapshot {
        val state = getState()
        val history = getAccessHistory()

        // Fallback: if currentHolder is null, use the latest history entry to avoid showing "None"
        val effectiveHolder = state.currentHolder ?: history.lastOrNull()?.nodeId

        val nodeIds = buildSet {
            effectiveHolder?.let { add(it) }
            history.forEach { add(it.nodeId) }
        }

        val nodes = nodeIds.map { id ->
            val csState = when {
                effectiveHolder == id -> "IN_CS"
                else -> "IDLE"
            }
            VisualizerNodeDto(
                id = id,
                state = csState
            )
        }

        val metricsDto = VisualizerMetricsDto(
            totalAccesses = accessMonitor.getTotalAccesses(),
            avgDurationMs = accessMonitor.getAverageDuration(),
            violationCount = state.violations.size
        )

        val bankAccount = resourceManager.getResource("bank-account") as? cs.resources.BankAccountResource
        val bankBalance = bankAccount?.getBalance() ?: 0L
        
        return VisualizerSnapshot(
            nodes = nodes,
            currentHolder = effectiveHolder,
            queue = emptyList(),
            accessHistory = history,
            metrics = metricsDto,
            bankBalance = bankBalance
        )
    }

    private fun notifyNodes() {
        val state = getState()
        scope.launch {
            NodeBroadcaster.broadcast(state)
        }
    }

    private fun notifyVisualizer() {
        if (!config.enableMonitoring) return

        val snapshot = buildSnapshot()
        scope.launch {
            VisualizerBroadcaster.broadcast(snapshot)
        }
    }
}
