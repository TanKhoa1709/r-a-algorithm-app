package app.core

import java.util.concurrent.atomic.AtomicLong

/**
 * Lamport logical clock implementation
 */
class LamportClock {
    private val clock = AtomicLong(0)
    
    /**
     * Get current clock value
     */
    fun getTime(): Long = clock.get()
    
    /**
     * Increment clock and return new value
     */
    fun tick(): Long = clock.incrementAndGet()
    
    /**
     * Update clock based on received timestamp
     */
    fun receive(timestamp: Long): Long {
        return clock.updateAndGet { maxOf(it, timestamp) + 1 }
    }
    
    /**
     * Reset clock (for testing)
     */
    fun reset() {
        clock.set(0)
    }
}

