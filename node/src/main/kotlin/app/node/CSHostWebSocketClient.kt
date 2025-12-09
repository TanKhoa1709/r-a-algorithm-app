package app.node

import app.models.CSState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Lightweight WebSocket client to subscribe to CS Host realtime updates.
 */
class CSHostWebSocketClient(
    private val csHostUrl: String,
    private val onState: (CSState) -> Unit,
    private val onError: (Throwable) -> Unit = {}
) {
    private val client = HttpClient(CIO) { install(WebSockets) }
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = CoroutineScope(Dispatchers.IO).launch {
            val wsUrl = csHostUrl.replaceFirst("http", "ws") + "/ws/cs-host"
            runCatching {
                client.ws(wsUrl) {
                    for (frame in incoming) {
                        if (!isActive) break
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            runCatching {
                                Json.decodeFromString<CSState>(text)
                            }.onSuccess { state ->
                                onState(state)
                            }.onFailure { ex ->
                                onError(ex)
                            }
                        }
                    }
                }
            }.onFailure {
                onError(it)
                // Silent retry can be added later
            }
        }
    }

    suspend fun stop() {
        job?.cancel()
        job = null
        client.close()
    }
}

