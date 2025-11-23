package cs.resources

import java.util.concurrent.atomic.AtomicLong

/**
 * Shared counter resource
 */
class SharedCounterResource(
    override val resourceId: String = "counter",
    override val resourceName: String = "Shared Counter"
) : SharedResource {
    private val counter = AtomicLong(0)
    private val accessCount = AtomicLong(0)
    private var lastAccessTime: Long? = null
    
    override suspend fun access(nodeId: String, requestId: String): ResourceAccessResult {
        val value = counter.incrementAndGet()
        accessCount.incrementAndGet()
        lastAccessTime = System.currentTimeMillis()
        
        return ResourceAccessResult(
            success = true,
            message = "Counter incremented to $value",
            data = mapOf("counter" to value)
        )
    }
    
    override suspend fun release(nodeId: String, requestId: String) {
        // No cleanup needed for counter
    }
    
    override fun getState(): ResourceState {
        return ResourceState(
            resourceId = resourceId,
            currentUser = null,
            accessCount = accessCount.get(),
            lastAccessTime = lastAccessTime,
            metadata = mapOf("counter" to counter.get())
        )
    }
    
    fun getValue(): Long = counter.get()
    fun reset() {
        counter.set(0)
        accessCount.set(0)
    }
}

