package cs

import app.models.CSState
import app.models.CSEntry
import app.models.Violation
import cs.monitor.AccessMonitor
import cs.monitor.ViolationDetector
import cs.resources.ResourceManager
import cs.resources.SharedResource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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
    private val accessQueue = ConcurrentLinkedQueue<String>()
    private val currentHolder: AtomicReference<String?> = AtomicReference(null)
    private val accessHistory = ConcurrentLinkedQueue<CSEntry>()
    private val totalAccesses = AtomicLong(0)
    private val violations = ConcurrentLinkedQueue<Violation>()
    private val accessMutex = Mutex()
    
    private val accessMonitor = AccessMonitor(config.enableMonitoring)
    private val violationDetector = ViolationDetector(config.enableViolationDetection)
    
    /**
     * Request access to critical section
     */
    suspend fun requestAccess(nodeId: String, requestId: String): Boolean {
        return accessMutex.withLock {
            if (currentHolder.get() == null && accessQueue.isEmpty()) {
                grantAccess(nodeId, requestId)
                true
            } else {
                accessQueue.offer(nodeId)
                false
            }
        }
    }
    
    /**
     * Grant access to critical section
     */
    private suspend fun grantAccess(nodeId: String, requestId: String) {
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
                val nextNode = accessQueue.poll()
                if (nextNode != null) {
                    grantAccess(nextNode, "")
                }
            }
        }
    }
    
    /**
     * Get current state
     */
    fun getState(): CSState {
        return CSState(
            isLocked = currentHolder.get() != null,
            currentHolder = currentHolder.get(),
            queue = accessQueue.toList(),
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
}

