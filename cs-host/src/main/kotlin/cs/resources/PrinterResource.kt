package cs.resources

import java.util.concurrent.atomic.AtomicLong

/**
 * Printer resource (simulated)
 */
class PrinterResource(
    override val resourceId: String = "printer",
    override val resourceName: String = "Shared Printer"
) : SharedResource {
    private val printQueue = mutableListOf<String>()
    private val accessCount = AtomicLong(0)
    private var lastAccessTime: Long? = null
    private var currentUser: String? = null
    
    override suspend fun access(nodeId: String, requestId: String): ResourceAccessResult {
        currentUser = nodeId
        accessCount.incrementAndGet()
        lastAccessTime = System.currentTimeMillis()
        
        val jobId = "job-${System.currentTimeMillis()}"
        val document = "Document from $nodeId"
        printQueue.add(document)
        
        return ResourceAccessResult(
            success = true,
            message = "Print job queued: $jobId",
            data = mapOf(
                "jobId" to jobId,
                "queueSize" to printQueue.size.toString()
            )
        )
    }
    
    override suspend fun release(nodeId: String, requestId: String) {
        if (currentUser == nodeId) {
            // Process print job
            if (printQueue.isNotEmpty()) {
                printQueue.removeAt(0)
            }
            currentUser = null
        }
    }
    
    override fun getState(): ResourceState {
        return ResourceState(
            resourceId = resourceId,
            currentUser = currentUser,
            accessCount = accessCount.get(),
            lastAccessTime = lastAccessTime,
            metadata = mapOf(
                "queueSize" to printQueue.size.toString(),
                "queue" to printQueue.joinToString(",")
            )
        )
    }
    
    fun getQueueSize(): Int = printQueue.size
    fun clearQueue() {
        printQueue.clear()
        currentUser = null
    }
}

