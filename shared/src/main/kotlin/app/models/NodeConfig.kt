package app.models

import kotlinx.serialization.Serializable
import java.net.InetAddress
import java.util.UUID

/**
 * Configuration for a node in the distributed system
 */
@Serializable
data class NodeConfig(
    val nodeId: String,
    val host: String = InetAddress.getLocalHost().hostName,
    val port: Int = 8080,
    val csHostUrl: String = "http://127.0.0.1:8080",
    val discoveryPort: Int = 8888,
    val heartbeatInterval: Long = 5000,
    val requestTimeout: Long = 30000
)
//TODO: need recheck after testing.