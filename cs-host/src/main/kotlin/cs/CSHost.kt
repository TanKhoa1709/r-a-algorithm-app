package cs

import app.models.CSState
import app.models.CSEntry
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
 * Main Critical Section Host coordinator
 */
class CSHost(
    private val config: CSHostConfig,
    private val resourceManager: ResourceManager
) {
    private data class AccessRequest(val nodeId: String, val requestId: String)

    private val accessQueue = ConcurrentLinkedQueue<AccessRequest>()
    private val currentHolder: AtomicReference<String?> = AtomicReference(null)
    private val accessHistory = ConcurrentLinkedQueue<CSEntry>()
    private val totalAccesses = AtomicLong(0)
    private val violations = ConcurrentLinkedQueue<Violation>()
    private val accessMutex = Mutex()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val accessMonitor = AccessMonitor(config.enableMonitoring)
    private val violationDetector = ViolationDetector(config.enableViolationDetection)

    /**
     * Request access to critical section
     */
    suspend fun requestAccess(nodeId: String, requestId: String): Boolean {
        var granted = false
        accessMutex.withLock {
            // Nếu node đã giữ CS hoặc đã có trong hàng đợi, không nhận thêm request mới
            if (currentHolder.get() == nodeId || accessQueue.any { it.nodeId == nodeId }) {
                granted = false
            } else if (currentHolder.get() == null && accessQueue.isEmpty()) {
                grantAccess(nodeId, requestId)
                granted = true
            } else {
                accessQueue.offer(AccessRequest(nodeId, requestId))
                granted = false
            }
        }

        notifyNodes()
        notifyVisualizer()
        return granted
    }

    /**
     * Grant access to critical section
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
     * Release critical section
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

                // Grant access to next in queue
                val nextRequest = accessQueue.poll()
                if (nextRequest != null) {
                    grantAccess(nextRequest.nodeId, nextRequest.requestId)
                }
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
            queue = accessQueue.map { it.nodeId }.toList(),
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
     * Xây snapshot gửi cho visualizer
     */
    private fun buildSnapshot(): VisualizerSnapshot {
        val state = getState()
        val history = getAccessHistory()

        // Tạm thời suy ra danh sách node từ history + queue + currentHolder
        val nodeIds = buildSet {
            addAll(state.queue)
            state.currentHolder?.let { add(it) }
            history.forEach { add(it.nodeId) }
        }

        val nodes = nodeIds.map { id ->
            val csState = when {
                state.currentHolder == id -> "HELD"
                state.queue.contains(id) -> "WANTED"
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

        return VisualizerSnapshot(
            nodes = nodes,
            currentHolder = state.currentHolder,
            queue = state.queue,
            accessHistory = history,
            metrics = metricsDto
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
