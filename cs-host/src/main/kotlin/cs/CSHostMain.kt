package cs

import cs.api.configureCSRoutes
import cs.api.configureWebSocketHandler
import cs.resources.ResourceManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json

fun main() {
    val config = CSHostConfig()
    val resourceManager = ResourceManager()
    val csHost = CSHost(config, resourceManager)

    embeddedServer(Netty, port = config.port) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        install(WebSockets)
        configureCSRoutes(csHost)
        configureWebSocketHandler(csHost)
    }.start(wait = true)
}

