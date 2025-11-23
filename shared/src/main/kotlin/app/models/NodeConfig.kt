package app.models

import kotlinx.serialization.Serializable

/**
 * Configuration for a node in the distributed system
 */
@Serializable
data class NodeConfig(
    val nodeId: String,
    val host: String,
    val port: Int,
    val csHostUrl: String,
    val discoveryPort: Int = 8888,
    val heartbeatInterval: Long = 5000,
    val requestTimeout: Long = 30000
)

