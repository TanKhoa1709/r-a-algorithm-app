package cs.resources

import kotlinx.serialization.Serializable

/**
 * Interface for shared resources in the critical section
 */
interface SharedResource {
    val resourceId: String
    val resourceName: String
    
    /**
    * Access the resource (called when node enters CS)
    */
    suspend fun access(nodeId: String, requestId: String): ResourceAccessResult
    
    /**
    * Release the resource (called when node exits CS)
    */
    suspend fun release(nodeId: String, requestId: String)
    
    /**
    * Get current state of the resource
    */
    fun getState(): ResourceState
}

/**
 * Result of resource access
 */
@Serializable
data class ResourceAccessResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, String> = emptyMap()
)

/**
 * State of a resource
 */
@Serializable
data class ResourceState(
    val resourceId: String,
    val currentUser: String?,
    val accessCount: Long,
    val lastAccessTime: Long?,
    val metadata: Map<String, String> = emptyMap()
)

