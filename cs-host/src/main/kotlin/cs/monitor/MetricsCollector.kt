package cs.monitor

import app.models.CSState
import java.util.concurrent.atomic.AtomicLong

/**
 * Collects metrics about the critical section
 */
class MetricsCollector {
    private val totalRequests = AtomicLong(0)
    private val totalReleases = AtomicLong(0)
    private val totalWaitTime = AtomicLong(0)
    private val maxWaitTime = AtomicLong(0)
    
    fun recordRequest() {
        totalRequests.incrementAndGet()
    }
    
    fun recordRelease() {
        totalReleases.incrementAndGet()
    }
    
    fun recordWaitTime(waitTime: Long) {
        totalWaitTime.addAndGet(waitTime)
        maxWaitTime.updateAndGet { maxOf(it, waitTime) }
    }
    
    fun getTotalRequests(): Long = totalRequests.get()
    fun getTotalReleases(): Long = totalReleases.get()
    fun getAverageWaitTime(): Double {
        val releases = totalReleases.get()
        return if (releases > 0) {
            totalWaitTime.get().toDouble() / releases
        } else {
            0.0
        }
    }
    
    fun getMaxWaitTime(): Long = maxWaitTime.get()
    
    fun getMetrics(state: CSState): Map<String, Any> {
        return mapOf(
            "totalRequests" to totalRequests.get(),
            "totalReleases" to totalReleases.get(),
            "averageWaitTime" to getAverageWaitTime(),
            "maxWaitTime" to maxWaitTime.get(),
            "queueSize" to state.queue.size,
            "isLocked" to state.isLocked,
            "currentHolder" to (state.currentHolder ?: "none")
        )
    }
    
    fun reset() {
        totalRequests.set(0)
        totalReleases.set(0)
        totalWaitTime.set(0)
        maxWaitTime.set(0)
    }
}

