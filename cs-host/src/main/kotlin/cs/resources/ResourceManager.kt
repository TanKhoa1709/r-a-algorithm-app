package cs.resources

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages all shared resources
 */
class ResourceManager {
    private val resources = ConcurrentHashMap<String, SharedResource>()
    
    init {
        // Register default resources
        registerResource(SharedCounterResource())
        registerResource(BankAccountResource())
        registerResource(DocumentResource())
        registerResource(PrinterResource())
    }
    
    /**
     * Register a new resource
     */
    fun registerResource(resource: SharedResource) {
        resources[resource.resourceId] = resource
    }
    
    /**
     * Get a resource by ID
     */
    fun getResource(resourceId: String): SharedResource? {
        return resources[resourceId]
    }
    
    /**
     * Get all resources
     */
    fun getAllResources(): List<SharedResource> {
        return resources.values.toList()
    }
    
    /**
     * Access a resource
     */
    suspend fun accessResource(resourceId: String, nodeId: String, requestId: String): ResourceAccessResult {
        val resource = resources[resourceId]
        return if (resource != null) {
            resource.access(nodeId, requestId)
        } else {
            ResourceAccessResult(
                success = false,
                message = "Resource not found: $resourceId"
            )
        }
    }
    
    /**
     * Release a resource
     */
    suspend fun releaseResource(resourceId: String, nodeId: String, requestId: String) {
        resources[resourceId]?.release(nodeId, requestId)
    }
}

