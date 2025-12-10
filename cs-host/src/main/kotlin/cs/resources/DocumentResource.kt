package cs.resources

import java.util.concurrent.atomic.AtomicLong

/**
 * Document resource (simulated shared document)
 */
class DocumentResource(
    override val resourceId: String = "document",
    override val resourceName: String = "Shared Document"
) : SharedResource {
    private val content = StringBuilder()
    private val accessCount = AtomicLong(0)
    private var lastAccessTime: Long? = null
    private var currentEditor: String? = null
    
    override suspend fun access(nodeId: String, requestId: String): ResourceAccessResult {
        currentEditor = nodeId
        accessCount.incrementAndGet()
        lastAccessTime = System.currentTimeMillis()
        
        val edit = "Edit by $nodeId at ${System.currentTimeMillis()}\n"
        content.append(edit)
        
        return ResourceAccessResult(
            success = true,
            message = "Document edited",
            data = mapOf(
                "contentLength" to content.length.toString(),
                "edit" to edit.trim()
            )
        )
    }
    
    override suspend fun release(nodeId: String, requestId: String) {
        if (currentEditor == nodeId) {
            currentEditor = null
        }
    }
    
    override fun getState(): ResourceState {
        return ResourceState(
            resourceId = resourceId,
            currentUser = currentEditor,
            accessCount = accessCount.get(),
            lastAccessTime = lastAccessTime,
            metadata = mapOf(
                "contentLength" to content.length.toString(),
                "lineCount" to content.lines().size.toString()
            )
        )
    }
    
    fun getContent(): String = content.toString()
    fun clear() {
        content.clear()
        currentEditor = null
    }
}

