package app.node

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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

fun main(args: Array<String>) {
    val configPath = args.getOrNull(0) ?: "config/nodes/node1.json"
    val configFile = File(configPath)

    val rawSharedConfig = if (configFile.exists()) {
        Json.decodeFromString<app.models.NodeConfig>(configFile.readText())
    } else {
        app.models.NodeConfig(
            nodeId = "node1",
            host = "auto",
            port = 8081,
            csHostUrl = "http://localhost:8080"
        )
    }

    val resolvedHost = resolveHost(rawSharedConfig.host)
    val sharedConfig = rawSharedConfig.copy(host = resolvedHost)

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
                title = "Ricart-Agrawala Node: ${sharedConfig.nodeId}"
            ) {
                NodeTheme {
                    NodeUI(
                        app.getController(),
                        currentCsHostUrl = sharedConfig.csHostUrl,
                        onUpdateCsHostUrl = { newUrl ->
                            app.updateCsHostUrl(newUrl)
                        }
                    )
                }
            }
        }
    } else {
        // Run headless
        Thread.sleep(Long.MAX_VALUE)
    }
}
