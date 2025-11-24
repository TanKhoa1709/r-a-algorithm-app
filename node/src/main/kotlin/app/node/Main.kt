package app.node

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.node.ui.theme.NodeTheme
import app.node.ui.NodeUI
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

fun main(args: Array<String>) {
    val configPath = args.getOrNull(0) ?: "config/nodes/node1.json"
    val configFile = File(configPath)
    
    val sharedConfig = if (configFile.exists()) {
        Json.decodeFromString<app.models.NodeConfig>(configFile.readText())
    } else {
        // Default config
        app.models.NodeConfig(
            nodeId = "node1",
            host = "localhost",
            port = 8081,
            csHostUrl = "http://localhost:8080"
        )
    }
    
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

