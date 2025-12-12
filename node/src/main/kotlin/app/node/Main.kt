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

private fun detectLocalIp(): String {
    val interfaces = NetworkInterface.getNetworkInterfaces()
        ?: return "127.0.0.1"

    for (ni in Collections.list(interfaces)) {
        if (!ni.isUp || ni.isLoopback || ni.isVirtual) continue

        val addresses = Collections.list(ni.inetAddresses)
        val ipv4 = addresses.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
        if (ipv4 != null) {
            return ipv4.hostAddress
        }
    }
    return "127.0.0.1"
}

private fun resolveHost(rawHost: String): String {
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
        autoConnect = true
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
                    NodeUI(app.getController())
                }
            }
        }
    } else {
        // Run headless
        Thread.sleep(Long.MAX_VALUE)
    }
}
