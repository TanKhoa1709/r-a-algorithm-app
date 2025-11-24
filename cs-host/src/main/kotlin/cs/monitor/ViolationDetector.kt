package cs.monitor

import app.models.CSState
import app.models.CSEntry
import app.models.Violation
import app.models.ViolationType
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Detects violations in critical section access
 */
class ViolationDetector(private val enabled: Boolean) {
    private val violations = ConcurrentLinkedQueue<Violation>()
    private val recentAccesses = ConcurrentLinkedQueue<CSEntry>()
    
    fun checkAccess(entry: CSEntry, state: CSState) {
        if (!enabled) return
        
        // Check for concurrent access
        if (state.isLocked && state.currentHolder != entry.nodeId) {
            recordViolation(
                ViolationType.CONCURRENT_ACCESS,
                entry.nodeId,
                "Attempted access while ${state.currentHolder} holds the lock"
            )
        }
        
        // Check for timeout (if entry has been in queue too long)
        val timeInQueue = System.currentTimeMillis() - entry.timestamp
        if (timeInQueue > 30000) { // 30 seconds timeout
            recordViolation(
                ViolationType.TIMEOUT,
                entry.nodeId,
                "Request timed out after ${timeInQueue}ms"
            )
        }
        
        recentAccesses.offer(entry)
        
        // Keep only last 100 entries
        while (recentAccesses.size > 100) {
            recentAccesses.poll()
        }
    }
    
    fun recordViolation(type: ViolationType, nodeId: String, description: String) {
        val violation = Violation(
            type = type,
            nodeId = nodeId,
            timestamp = System.currentTimeMillis(),
            description = description
        )
        violations.offer(violation)
    }
    
    fun getViolations(): List<Violation> = violations.toList()
    
    fun getViolationsByType(): Map<ViolationType, Long> {
        return violations.groupingBy { it.type }.eachCount().mapValues { it.value.toLong() }
    }
    
    fun clear() {
        violations.clear()
        recentAccesses.clear()
    }
}

