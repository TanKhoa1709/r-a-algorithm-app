package app.net.discovery

import kotlinx.serialization.Serializable

/**
 * Configuration for service discovery
 */
@Serializable
data class DiscoveryConfig(
    val multicastAddress: String = "239.255.255.250",
    val multicastPort: Int = 8888,
    val discoveryInterval: Long = 5000,
    val nodeTimeout: Long = 15000,
    val bindInterface: String? = null  // Optional: bind to specific network interface IP
)

