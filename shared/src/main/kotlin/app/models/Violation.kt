package app.models

import kotlinx.serialization.Serializable

/**
 * Represents a violation detected in the critical section access
 */
@Serializable
data class Violation(
    val type: ViolationType,
    val nodeId: String,
    val timestamp: Long,
    val description: String
)

@Serializable
enum class ViolationType {
    CONCURRENT_ACCESS,
    TIMEOUT,
    PROTOCOL_VIOLATION
}

