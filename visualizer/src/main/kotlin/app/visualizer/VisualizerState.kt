package app.visualizer

enum class NodeState {
    IDLE,
    WANTED,
    HELD
}

data class NodeInfo(
    val id: String,
    val host: String = "",
    val port: Int = 0,
    val state: NodeState = NodeState.IDLE,
    val lastRequestTime: String? = null
)

data class Metrics(
    val totalRequests: Int = 0,
    val totalCsEntries: Int = 0,
    val avgWaitMs: Long = 0L,
    val violationCount: Int = 0
)

data class VisualizerState(
    val nodes: List<NodeInfo> = emptyList(),
    val currentCsHolder: String? = null,
    val queue: List<String> = emptyList(),
    val logLines: List<String> = emptyList(),
    val metrics: Metrics = Metrics()
)
