package app.node

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.models.NodeConfig as SharedNodeConfig
import app.node.ui.theme.NodeTheme
import app.node.ui.NodeUI
import kotlinx.serialization.json.Json
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Detect a usable IPv4 address for this host.
 * Preference order:
 * 1) NODE_HOST env var (explicit override)
 * 2) First site-local IPv4 on a non-virtual, non-loopback, non-point-to-point interface
 *    with a "normal" name (eth/en/wl/Ethernet/wlan/lan)
 * 3) Any other non-loopback IPv4
 * 4) Fallback 127.0.0.1
 */
private fun detectLocalIp(): String {
    // Explicit override via environment
    System.getenv("NODE_HOST")?.takeIf { it.isNotBlank() }?.let { return it.trim() }

    val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
    val ifaces = Collections.list(interfaces)
        .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.isPointToPoint }
        .filter { ni ->
            val name = ni.displayName.lowercase()
            val shortName = ni.name.lowercase()
            // De-prioritize obvious virtual/docker adapters
            listOf("docker", "veth", "virtual", "loopback").none { tag ->
                name.contains(tag) || shortName.contains(tag)
            }
        }

    // Helper to score interface names (prefer ethernet/wifi)
    fun scoreInterface(ni: NetworkInterface): Int {
        val n = ni.name.lowercase()
        val d = ni.displayName.lowercase()
        return when {
            n.startsWith("eth") || n.startsWith("en") -> 3
            n.startsWith("wl") || n.startsWith("wlan") || d.contains("wifi") -> 2
            d.contains("ethernet") || d.contains("lan") -> 2
            else -> 1
        }
    }

    // Collect candidate IPv4 addresses
    val candidates = ifaces.flatMap { ni ->
        Collections.list(ni.inetAddresses)
            .filterIsInstance<Inet4Address>()
            .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            .map { addr ->
                Triple(scoreInterface(ni), addr.isSiteLocalAddress, addr.hostAddress)
            }
    }

    // Prefer highest score, then site-local, then keep first
    val best = candidates.maxWithOrNull(
        compareBy<Triple<Int, Boolean, String>> { it.first } // interface score
            .thenByDescending { it.second } // site-local
    )

    if (best != null) return best.third

    // Fallback: any non-loopback IPv4
    for (ni in ifaces) {
        val ipv4 = Collections.list(ni.inetAddresses)
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
        if (ipv4 != null) return ipv4.hostAddress
    }

    return "127.0.0.1"
}

private fun resolveHost(rawHost: String): String {
    // Allow env override for host binding / advertisement
    System.getenv("NODE_HOST")?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    return if (rawHost == "auto") detectLocalIp() else rawHost
}

/**
 * Resolve config file path, supporting both Windows and Linux.
 * Tries multiple locations:
 * 1. Absolute path (if provided)
 * 2. Relative to current working directory
 * 3. Relative to project root (detected by finding config/ directory)
 */
private fun resolveConfigFile(configPath: String): File {
    val file = File(configPath)
    
    // If absolute path and exists, use it
    if (file.isAbsolute && file.exists()) {
        return file
    }
    
    // If relative path exists in current directory, use it
    if (file.exists()) {
        return file
    }
    
    // Try to find project root by looking for config/ directory
    // Start from current working directory and go up
    var currentDir = File(System.getProperty("user.dir"))
    var maxDepth = 10 // Prevent infinite loop
    var depth = 0
    
    while (depth < maxDepth) {
        val configDir = File(currentDir, "config")
        if (configDir.exists() && configDir.isDirectory) {
            // Found project root, try config file from here
            val projectConfigFile = File(currentDir, configPath)
            if (projectConfigFile.exists()) {
                return projectConfigFile
            }
        }
        
        val parent = currentDir.parentFile
        if (parent == null || parent == currentDir) {
            break // Reached filesystem root
        }
        currentDir = parent
        depth++
    }
    
    // If still not found, try from user.dir directly (Gradle project root)
    val projectRootFile = File(System.getProperty("user.dir"), configPath)
    if (projectRootFile.exists()) {
        return projectRootFile
    }
    
    // Return original file (will throw error if not exists)
    return file
}

fun main(args: Array<String>) {
    val configPath = args.getOrNull(0) ?: "config/nodes/node.json"
    val configFile = resolveConfigFile(configPath)
    
    if (!configFile.exists()) {
        System.err.println("ERROR: Config file not found: $configPath")
        System.err.println("  Tried: ${configFile.absolutePath}")
        System.err.println("  Current working directory: ${System.getProperty("user.dir")}")
        System.err.println("  Please specify correct path:")
        System.err.println("    Windows: .\\gradlew :node:run --args \"config\\nodes\\node1.json\"")
        System.err.println("    Linux:   ./gradlew :node:run --args \"config/nodes/node1.json\"")
        System.exit(1)
    }

    val rawSharedConfig: SharedNodeConfig = try {
        Json.decodeFromString<SharedNodeConfig>(configFile.readText())
    } catch (e: Exception) {
        System.err.println("ERROR: Failed to parse config file: ${configFile.absolutePath}")
        System.err.println("  Error: ${e.message}")
        System.exit(1)
        // This will never execute but satisfies type checker
        error("Config parsing failed")
    }

    // Only use csHostUrl from config file, no environment variable override
    val resolvedHost = resolveHost(rawSharedConfig.host)
    val sharedConfig = rawSharedConfig.copy(host = resolvedHost)
    
    // Log configuration for debugging
    println("Node Configuration:")
    println("  Node ID: ${sharedConfig.nodeId}")
    println("  Host: ${sharedConfig.host}:${sharedConfig.port}")
    println("  Bank Host URL: ${sharedConfig.csHostUrl} (from config file)")
    println("  Config file: ${configFile.absolutePath}")

    val nodeConfig = NodeConfig(
        sharedConfig = sharedConfig,
        uiEnabled = true,
        autoConnect = true,
        configPath = configPath
    )

    val app = NodeApplication(nodeConfig)
    app.start()

    if (nodeConfig.uiEnabled) {
        application {
            Window(
                onCloseRequest = {
                    app.stop()
                    exitApplication()
                },
                title = "Bank Branch: ${sharedConfig.nodeId}"
            ) {
                NodeTheme {
                    NodeUI(app.getController())
                }
            }
        }
    } else {
        // Run headless
        Thread.sleep(Long.MAX_VALUE)
    }
}
