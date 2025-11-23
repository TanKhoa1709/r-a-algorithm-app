package app.models

import kotlinx.serialization.Serializable

/**
 * Current state of the critical section
 */
@Serializable
data class CSState(
    val isLocked: Boolean,
    val currentHolder: String?,
    val queue: List<String>,
    val totalAccesses: Long,
    val violations: List<Violation>
)

