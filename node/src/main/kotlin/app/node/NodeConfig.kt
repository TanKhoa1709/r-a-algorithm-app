package app.node

import app.models.NodeConfig as SharedNodeConfig

/**
 * Node-specific configuration
 */
data class NodeConfig(
    val sharedConfig: SharedNodeConfig,
    val uiEnabled: Boolean = true,
    val autoConnect: Boolean = true
)

