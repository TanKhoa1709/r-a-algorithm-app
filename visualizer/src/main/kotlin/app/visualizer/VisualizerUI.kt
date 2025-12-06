package app.visualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun VisualizerScreen(state: VisualizerState) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cột trái: topology + queue
                Column(
                    modifier = Modifier.weight(2f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TopologyView(state, modifier = Modifier.weight(1f))
                    QueueView(state, modifier = Modifier)
                }

                // Cột phải: log + metrics
                Column(
                    modifier = Modifier.weight(3f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LogPanel(state, modifier = Modifier.weight(1f))
                    MetricsPanel(state.metrics, modifier = Modifier)
                }
            }
        }
    }
}

@Composable
private fun TopologyView(state: VisualizerState, modifier: Modifier = Modifier) {
    Card(
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth().fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Topology & CS State",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Current CS holder: ${state.currentCsHolder ?: "None"}",
                style = MaterialTheme.typography.body1
            )

            Spacer(Modifier.height(8.dp))

            // List các node
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.nodes) { node ->
                    NodeCard(node, isHolder = node.id == state.currentCsHolder)
                }
            }
        }
    }
}

@Composable
private fun NodeCard(node: NodeInfo, isHolder: Boolean) {
    val bgAlpha = when (node.state) {
        NodeState.IDLE -> 0.05f
        NodeState.WANTED -> 0.12f
        NodeState.HELD -> 0.20f
    }

    Card(
        elevation = if (isHolder) 6.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.primary.copy(alpha = bgAlpha))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.id + if (isHolder) " (IN CS)" else "",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${node.host}:${node.port}",
                    style = MaterialTheme.typography.caption
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "State: ${node.state}",
                    style = MaterialTheme.typography.body2
                )
                node.lastRequestTime?.let {
                    Text(
                        text = "Last req: $it",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueView(state: VisualizerState, modifier: Modifier.Companion) {
    Card(
        elevation = 4.dp,
        modifier = modifier.fillMaxWidth().heightIn(min = 120.dp)
    ) {
        Column(
            modifier = modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Waiting Queue",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            if (state.queue.isEmpty()) {
                Text("No node is waiting.", style = MaterialTheme.typography.body2)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.queue) { id ->
                        Text("• $id", style = MaterialTheme.typography.body1)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogPanel(state: VisualizerState, modifier: Modifier = Modifier) {
    Card(
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth().fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Text(
                "Event Log",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(state.logLines) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsPanel(metrics: Metrics, modifier: Modifier = Modifier) {
    Card(
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Metrics",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Text("Total requests: ${metrics.totalRequests}")
            Text("Total CS entries: ${metrics.totalCsEntries}")
            Text("Average wait: ${metrics.avgWaitMs} ms")
            Text("Violations: ${metrics.violationCount}")
        }
    }
}
