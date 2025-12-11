package app.visualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visualizer.theme.VisualizerColors
import app.visualizer.theme.VisualizerTheme

@Composable
fun VisualizerScreen(state: VisualizerState) {
    VisualizerTheme {
        // Main container with gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VisualizerColors.Background,
                            VisualizerColors.BackgroundGradientStart,
                            VisualizerColors.BackgroundGradientEnd
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            ) {
                // Premium header with accent bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decorative accent bar
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        VisualizerColors.PrimaryGradientStart,
                                        VisualizerColors.PrimaryGradientEnd
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "R&A CS Visualizer",
                            style = MaterialTheme.typography.h4.copy(
                                fontSize = 26.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = VisualizerColors.TextPrimary
                        )
                        Text(
                            text = "Real-time Critical Section Monitoring",
                            style = MaterialTheme.typography.caption,
                            color = VisualizerColors.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Cột trái: topology + queue
                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        TopologyView(
                            state = state,
                            modifier = Modifier.weight(1f)
                        )
                        QueueView(
                            state = state,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Cột phải: log + metrics
                    Column(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        LogPanel(
                            state = state,
                            modifier = Modifier.weight(1f)
                        )
                        MetricsPanel(
                            metrics = state.metrics,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeCard(node: NodeInfo, isHolder: Boolean) {
    val (bgColor, textColor) = when (node.state) {
        NodeState.IDLE -> Pair(VisualizerColors.IdleState, VisualizerColors.IdleStateText)
        NodeState.WANTED -> Pair(VisualizerColors.WantedState, VisualizerColors.WantedStateText)
        NodeState.HELD -> Pair(VisualizerColors.HeldState, VisualizerColors.HeldStateText)
    }

    Card(
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VisualizerColors.CardBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor.copy(alpha = 0.4f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = node.id,
                        fontWeight = FontWeight.SemiBold,
                        color = VisualizerColors.TextPrimary,
                        fontSize = 15.sp
                    )
                    if (isHolder) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VisualizerColors.HeldState)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "IN CS",
                                color = VisualizerColors.HeldStateText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${node.host}:${node.port}",
                    style = MaterialTheme.typography.caption,
                    color = VisualizerColors.TextMuted
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = node.state.name,
                        style = MaterialTheme.typography.caption,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                node.lastRequestTime?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last req: $it",
                        style = MaterialTheme.typography.caption,
                        color = VisualizerColors.TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun TopologyView(
    state: VisualizerState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = VisualizerColors.CardShadow,
                spotColor = VisualizerColors.CardShadow
            )
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VisualizerColors.CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        backgroundColor = VisualizerColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = VisualizerColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Topology & CS State",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = VisualizerColors.TextPrimary
                )
            }

            // CS Holder info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(VisualizerColors.SurfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Current CS holder:",
                    style = MaterialTheme.typography.body2,
                    color = VisualizerColors.TextSecondary
                )
                Text(
                    text = state.currentCsHolder ?: "None",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.currentCsHolder != null) VisualizerColors.HeldStateText else VisualizerColors.TextMuted
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.nodes) { node ->
                    NodeCard(node, isHolder = node.id == state.currentCsHolder)
                }
            }
        }
    }
}

@Composable
private fun QueueView(
    state: VisualizerState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = VisualizerColors.CardShadow,
                spotColor = VisualizerColors.CardShadow
            )
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VisualizerColors.CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        backgroundColor = VisualizerColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = VisualizerColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Waiting Queue",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = VisualizerColors.TextPrimary
                )
                if (state.queue.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VisualizerColors.WantedState)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = state.queue.size.toString(),
                            style = MaterialTheme.typography.caption,
                            color = VisualizerColors.WantedStateText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (state.queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VisualizerColors.SurfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No node is waiting.",
                        style = MaterialTheme.typography.body2,
                        color = VisualizerColors.TextMuted
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.queue.withIndex().toList()) { (index, id) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(VisualizerColors.SurfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(VisualizerColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.caption,
                                    color = VisualizerColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = id,
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Medium,
                                color = VisualizerColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogPanel(
    state: VisualizerState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = VisualizerColors.CardShadow,
                spotColor = VisualizerColors.CardShadow
            )
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VisualizerColors.CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        backgroundColor = VisualizerColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header with icon and count badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = VisualizerColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Event Log",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = VisualizerColors.TextPrimary
                )
                if (state.logEntries.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VisualizerColors.Primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = state.logEntries.size.toString(),
                            style = MaterialTheme.typography.caption,
                            color = VisualizerColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (state.logEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VisualizerColors.SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No events yet",
                        style = MaterialTheme.typography.body2,
                        color = VisualizerColors.TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.logEntries.reversed()) { entry ->
                        LogEntryCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry) {
    val isCompleted = entry.exitTime != null
    val statusColor = if (isCompleted) VisualizerColors.Success else VisualizerColors.Warning
    val statusBg = if (isCompleted) VisualizerColors.SuccessLight else VisualizerColors.WarningLight
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, VisualizerColors.CardBorder, RoundedCornerShape(10.dp))
            .background(VisualizerColors.SurfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row: Node ID and Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.nodeId,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                    color = VisualizerColors.TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isCompleted) "COMPLETED" else "ACTIVE",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    fontSize = 10.sp
                )
            }
        }
        
        // Request ID row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Request: ",
                style = MaterialTheme.typography.caption,
                color = VisualizerColors.TextMuted
            )
            Text(
                text = entry.requestId,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                color = VisualizerColors.TextSecondary
            )
        }
        
        Divider(color = VisualizerColors.Divider, thickness = 1.dp)
        
        // Time details grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Entry time
            Column {
                Text(
                    text = "Enter",
                    style = MaterialTheme.typography.caption,
                    color = VisualizerColors.TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = formatTimestamp(entry.entryTime),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = VisualizerColors.TextPrimary
                )
            }
            
            // Exit time
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Exit",
                    style = MaterialTheme.typography.caption,
                    color = VisualizerColors.TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = entry.exitTime?.let { formatTimestamp(it) } ?: "—",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = if (entry.exitTime != null) VisualizerColors.TextPrimary else VisualizerColors.TextMuted
                )
            }
            
            // Duration
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.caption,
                    color = VisualizerColors.TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = entry.duration?.let { "${it}ms" } ?: "—",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.duration != null) VisualizerColors.Primary else VisualizerColors.TextMuted
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    formatter.timeZone = java.util.TimeZone.getTimeZone("GMT+7")
    return formatter.format(java.util.Date(timestamp)) + " +07"
}

@Composable
private fun MetricsPanel(
    metrics: Metrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = VisualizerColors.CardShadow,
                spotColor = VisualizerColors.CardShadow
            )
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, VisualizerColors.CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        backgroundColor = VisualizerColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = VisualizerColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Metrics",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = VisualizerColors.TextPrimary
                )
            }

            // Metrics grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricItem("Total requests", metrics.totalRequests.toString())
                MetricItem("Total CS entries", metrics.totalCsEntries.toString())
                MetricItem("Average wait", "${metrics.avgWaitMs} ms")
                MetricItem(
                    label = "Violations",
                    value = metrics.violationCount.toString(),
                    isAlert = metrics.violationCount > 0
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    isAlert: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isAlert) VisualizerColors.ErrorLight.copy(alpha = 0.5f)
                else VisualizerColors.SurfaceVariant
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = if (isAlert) VisualizerColors.Error else VisualizerColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.SemiBold,
            color = if (isAlert) VisualizerColors.Error else VisualizerColors.TextPrimary
        )
    }
}

