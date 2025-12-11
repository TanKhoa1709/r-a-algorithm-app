package app.visualizer

import app.models.VisualizerSnapshot
import app.models.VisualizerMetricsDto
import app.models.VisualizerNodeDto
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class VisualizerClient(
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val _state = MutableStateFlow(VisualizerState())
    val state: StateFlow<VisualizerState> get() = _state

    fun connect(url: String) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    client.webSocket(urlString = url) {
                        // nhận snapshot liên tục
                        for (frame in incoming) {
                            val text = (frame as? Frame.Text)?.readText() ?: continue
                            val snapshot = json.decodeFromString(VisualizerSnapshot.serializer(), text)
                            val uiState = snapshot.toUiState()
                            _state.value = uiState
                        }
                    }
                } catch (e: Exception) {
                    // TODO: log lỗi, chờ 1 chút rồi reconnect
                    kotlinx.coroutines.delay(2000)
                }
            }
        }
    }

    fun close() {
        scope.cancel()
        client.close()
    }
}

// Convert DTO shared -> UI state
private fun VisualizerSnapshot.toUiState(): VisualizerState =
    VisualizerState(
        nodes = this.nodes.map { it.toNodeInfo() },
        currentCsHolder = this.currentHolder,
        queue = this.queue,
        logEntries = this.accessHistory.map { entry ->
            LogEntry(
                nodeId = entry.nodeId,
                requestId = entry.requestId,
                timestamp = entry.timestamp,
                entryTime = entry.entryTime,
                exitTime = entry.exitTime,
                duration = entry.duration
            )
        },
        metrics = this.metrics.toMetrics()
    )

private fun VisualizerNodeDto.toNodeInfo(): NodeInfo =
    NodeInfo(
        id = this.id,
        host = "",       // tạm để trống
        port = 0,
        state = when (this.state) {
            "WANTED" -> NodeState.WANTED
            "HELD" -> NodeState.HELD
            else -> NodeState.IDLE
        },
        lastRequestTime = null
    )

private fun VisualizerMetricsDto.toMetrics(): Metrics =
    Metrics(
        totalRequests = this.totalAccesses.toInt(),
        totalCsEntries = this.totalAccesses.toInt(),
        avgWaitMs = this.avgDurationMs.toLong(),
        violationCount = this.violationCount
    )
