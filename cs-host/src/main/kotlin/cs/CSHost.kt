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
 * Critical Section Host - Resource Manager (NOT a coordinator)
 * 
 * CS Host chỉ quản lý tài nguyên dùng chung (resources), KHÔNG phải coordinator của Ricart-Agrawala algorithm.
 * Ricart-Agrawala algorithm chạy distributed giữa các nodes để quyết định thứ tự vào CS.
 * CS Host chỉ đảm bảo mutual exclusion ở tầng resource access.
 */
class CSHost(
    private val config: CSHostConfig,
    private val resourceManager: ResourceManager
) {
    // Chỉ track node đang sử dụng resource, KHÔNG có queue
    private val currentHolder: AtomicReference<String?> = AtomicReference(null)
    private val accessHistory = ConcurrentLinkedQueue<CSEntry>()
    private val totalAccesses = AtomicLong(0)
    private val violations = ConcurrentLinkedQueue<Violation>()
    private val accessMutex = Mutex()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val accessMonitor = AccessMonitor(config.enableMonitoring)
    private val violationDetector = ViolationDetector(config.enableViolationDetection)

    /**
     * Request access to resource (NOT coordinator - chỉ kiểm tra resource availability)
     * 
     * Node chỉ gọi method này SAU KHI đã vào CS theo Ricart-Agrawala algorithm.
     * CS Host chỉ kiểm tra xem resource có đang được sử dụng không.
     * 
     * @return true nếu resource available, false nếu đang được sử dụng
     */
    suspend fun requestAccess(nodeId: String, requestId: String): Boolean {
        val granted = accessMutex.withLock {
            // Chỉ kiểm tra xem resource có đang được sử dụng không
            // KHÔNG có queue, KHÔNG quyết định thứ tự vào CS
            when {
                currentHolder.get() == null -> {
                    // Resource available - grant access
                    grantAccess(nodeId, requestId)
                    true
                }
                currentHolder.get() == nodeId -> {
                    // Node đã đang giữ resource (có thể là duplicate request)
                    true
                }
                else -> {
                    // Resource đang được sử dụng bởi node khác
                    // Node phải đợi Ricart-Agrawala algorithm quyết định
                    false
                }
            }
        }

        notifyNodes()
        notifyVisualizer()
        return granted
    }

    /**
     * Grant access to resource (record access, không phải coordinator decision)
     */
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

    /**
     * Release resource access
     * 
     * Node gọi method này khi exit CS.
     * KHÔNG grant cho node tiếp theo - Ricart-Agrawala algorithm sẽ quyết định.
     */
    suspend fun releaseAccess(nodeId: String, requestId: String) {
        accessMutex.withLock {
            if (currentHolder.get() == nodeId) {
                val exitTime = System.currentTimeMillis()
                currentHolder.set(null)

                // Update entry
                val entry = accessHistory.lastOrNull { it.nodeId == nodeId && it.requestId == requestId }
                entry?.let {
                    val updated = it.copy(
                        exitTime = exitTime,
                        duration = exitTime - it.entryTime
                    )
                    accessHistory.remove(it)
                    accessHistory.offer(updated)
                }
                
                // KHÔNG grant cho node tiếp theo - Ricart-Agrawala algorithm quyết định
            }
        }
        notifyNodes()
        notifyVisualizer()
    }

    /**
     * Get current state
     */
    fun getState(): CSState {
        return CSState(
            isLocked = currentHolder.get() != null,
            currentHolder = currentHolder.get(),
            queue = emptyList(), // KHÔNG có queue - Ricart-Agrawala quyết định thứ tự
            totalAccesses = totalAccesses.get(),
            violations = violations.toList()
        )
    }

    /**
     * Get access history
     */
    fun getAccessHistory(): List<CSEntry> = accessHistory.toList()

    /**
     * Record violation
     */
    fun recordViolation(violation: Violation) {
        violations.offer(violation)
    }

    /**
     * Get resource manager
     */
    fun getResourceManager(): ResourceManager = resourceManager
    
    /**
     * Withdraw money from bank account
     * Node phải đã vào CS theo Ricart-Agrawala trước khi gọi method này
     * @return TransactionResult với success và balance mới
     */
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
        
        // Thực hiện withdraw - chỉ rút được nếu đủ tiền
        val currentBalance = bankAccount.getBalance()
        val success = bankAccount.withdraw(amount)
        val newBalance = bankAccount.getBalance()
        
        // Ghi log lịch sử (ghi cả khi fail để track)
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
    
    /**
     * Deposit money to bank account
     * Node phải đã vào CS theo Ricart-Agrawala trước khi gọi method này
     * @return TransactionResult với success và balance mới
     */
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
        
        // Thực hiện deposit
        val success = bankAccount.deposit(amount)
        val newBalance = bankAccount.getBalance()
        
        // Ghi log lịch sử
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

    /**
     * Xây snapshot gửi cho visualizer
     */
    fun buildSnapshot(): VisualizerSnapshot {
        val state = getState()
        val history = getAccessHistory()

        // Suy ra danh sách node từ history + currentHolder (KHÔNG có queue)
        val nodeIds = buildSet {
            state.currentHolder?.let { add(it) }
            history.forEach { add(it.nodeId) }
        }

        val nodes = nodeIds.map { id ->
            val csState = when {
                state.currentHolder == id -> "IN_CS"  // Node đang trong CS (theo Ricart-Agrawala) và đang access resource
                else -> "IDLE"  // Node không trong CS hoặc đang chờ Ricart-Agrawala quyết định
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

        // Get bank balance
        val bankAccount = resourceManager.getResource("bank-account") as? cs.resources.BankAccountResource
        val bankBalance = bankAccount?.getBalance() ?: 0L
        
        return VisualizerSnapshot(
            nodes = nodes,
            currentHolder = state.currentHolder,
            queue = emptyList(), // KHÔNG có queue
            accessHistory = history,
            metrics = metricsDto,
            bankBalance = bankBalance
        )
    }

    /**
     * Push CSState updates to all connected nodes via WebSocket
     */
    private fun notifyNodes() {
        val state = getState()
        scope.launch {
            NodeBroadcaster.broadcast(state)
        }
    }

    /**
     * Gửi snapshot tới tất cả visualizer (chạy nền)
     */
    private fun notifyVisualizer() {
        if (!config.enableMonitoring) return // nếu bạn muốn chỉ bật khi monitoring

        val snapshot = buildSnapshot()
        scope.launch {
            VisualizerBroadcaster.broadcast(snapshot)
        }
    }
}
