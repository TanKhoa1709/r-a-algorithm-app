package app.visualizer

enum class NodeState {
    IDLE,              // Node không request CS
    REQUESTING,        // Node đã gửi REQUEST, đang chờ replies
    WAITING_REPLIES,   // Node đang chờ replies từ các nodes khác
    IN_CS,             // Node đã vào CS (theo Ricart-Agrawala) và đang access resource
    HELD               // Alias cho IN_CS (backward compatibility)
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

data class LogEntry(
    val nodeId: String,
    val requestId: String,
    val timestamp: Long,
    val entryTime: Long,
    val exitTime: Long? = null,
    val duration: Long? = null
)

data class VisualizerState(
    val nodes: List<NodeInfo> = emptyList(),
    val currentCsHolder: String? = null,
    val queue: List<String> = emptyList(),  // Deprecated - không còn queue, giữ để backward compatibility
    val logEntries: List<LogEntry> = emptyList(),
    val metrics: Metrics = Metrics()
)
