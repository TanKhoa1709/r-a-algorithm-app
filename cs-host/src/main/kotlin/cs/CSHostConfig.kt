package cs

import kotlinx.serialization.Serializable

/**
 * Configuration for Critical Section Host
 */
@Serializable
data class CSHostConfig(
    val port: Int = 8080,
    val maxConcurrentAccess: Int = 1,
    val accessTimeout: Long = 30000,
    val enableMonitoring: Boolean = true,
    val enableViolationDetection: Boolean = true
)

