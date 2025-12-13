package app.models

import kotlinx.serialization.Serializable

@Serializable
data class VisualizerNodeDto(
    val id: String,
    val state: String      // "IDLE" | "REQUESTING" | "WAITING_REPLIES" | "IN_CS" | "HELD"
)

@Serializable
data class VisualizerMetricsDto(
    val totalAccesses: Long = 0,
    val avgDurationMs: Double = 0.0,
    val violationCount: Int = 0
)

@Serializable
data class VisualizerSnapshot(
    val nodes: List<VisualizerNodeDto>,
    val currentHolder: String? = null,
    val queue: List<String> = emptyList(),
    val accessHistory: List<CSEntry> = emptyList(),
    val metrics: VisualizerMetricsDto = VisualizerMetricsDto()
)

