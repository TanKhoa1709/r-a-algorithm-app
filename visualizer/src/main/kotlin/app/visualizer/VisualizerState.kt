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

// Data giả để test UI, sau này bạn thay bằng data thật từ WebSocket
fun sampleVisualizerState(): VisualizerState {
    return VisualizerState(
        nodes = listOf(
            NodeInfo("node1", "192.168.1.10", 8081, NodeState.HELD, "12:00:05"),
            NodeInfo("node2", "192.168.1.11", 8082, NodeState.WANTED, "12:00:07"),
            NodeInfo("node3", "192.168.1.12", 8083, NodeState.IDLE, null),
            NodeInfo("node4", "192.168.1.13", 8084, NodeState.IDLE, null),
            NodeInfo("node5", "192.168.1.14", 8085, NodeState.IDLE, null)
        ),
        currentCsHolder = "node1",
        queue = listOf("node2"),
        logLines = listOf(
            "[11:59:58] node1 -> REQUEST [ts=5]",
            "[11:59:59] node2 -> REQUEST [ts=6]",
            "[12:00:00] node3 -> REQUEST [ts=7]",
            "[12:00:01] node1 <- REPLY from node2",
            "[12:00:02] node1 <- REPLY from node3",
            "[12:00:03] node1 ENTER CS (PrinterResource)",
            "[12:00:06] node3 CANCEL REQUEST",
        ),
        metrics = Metrics(
            totalRequests = 7,
            totalCsEntries = 3,
            avgWaitMs = 1200L,
            violationCount = 0
        )
    )
}
