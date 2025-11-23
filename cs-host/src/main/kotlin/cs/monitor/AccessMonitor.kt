package cs.monitor

import app.models.CSEntry
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Monitors access to critical section
 */
class AccessMonitor(private val enabled: Boolean) {
    private val accessLog = ConcurrentLinkedQueue<CSEntry>()
    private val totalAccesses = AtomicLong(0)
    private val totalDuration = AtomicLong(0)
    
    fun recordAccess(entry: CSEntry) {
        if (!enabled) return
        
        accessLog.offer(entry)
        totalAccesses.incrementAndGet()
        
        entry.duration?.let {
            totalDuration.addAndGet(it)
        }
    }
    
    fun getAccessLog(): List<CSEntry> = accessLog.toList()
    
    fun getTotalAccesses(): Long = totalAccesses.get()
    
    fun getAverageDuration(): Double {
        val accesses = totalAccesses.get()
        return if (accesses > 0) {
            totalDuration.get().toDouble() / accesses
        } else {
            0.0
        }
    }
    
    fun getAccessCountByNode(): Map<String, Long> {
        return accessLog.groupingBy { it.nodeId }.eachCount()
    }
    
    fun clear() {
        accessLog.clear()
        totalAccesses.set(0)
        totalDuration.set(0)
    }
}

