package app.node.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.node.controller.NodeController
import app.node.ui.components.*
import app.node.ui.theme.NodeColors
import kotlinx.coroutines.launch

/**
 * Main UI for the node application
 */
@Composable
fun NodeUI(
    controller: NodeController
) {
    var inCS by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()
    var hasPendingRequest by remember { mutableStateOf(false) }
    var connectedPeers by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        while (true) {
            clock = controller.getClock()
            inCS = controller.isInCriticalSection()
            hasPendingRequest = controller.hasPendingRequest()
            connectedPeers = controller.getConnectedNodes()
            kotlinx.coroutines.delay(100)
        }
    }

    // Main container with gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NodeColors.Background,
                        NodeColors.BackgroundGradientStart,
                        NodeColors.BackgroundGradientEnd
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
                                    NodeColors.PrimaryGradientStart,
                                    NodeColors.PrimaryGradientEnd
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Bank Branch",
                        style = MaterialTheme.typography.h4.copy(
                            fontSize = 26.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = NodeColors.TextPrimary
                    )
                    Text(
                        text = "Distributed Bank System",
                        style = MaterialTheme.typography.caption,
                        color = NodeColors.TextMuted
                    )
                    Text(
                        text = "IP: ${controller.getNodeHost()}:${controller.getNodePort()}",
                        style = MaterialTheme.typography.caption,
                        color = NodeColors.TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status Bar - simplified
            StatusBar(
                inCriticalSection = inCS,
                clock = clock,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            ControlPanel(
                onWithdraw = { amount ->
                    scope.launch {
                        try {
                            controller.withdraw(amount)
                        } catch (e: Exception) {
                            // Error already logged in controller
                        }
                    }
                },
                onDeposit = { amount ->
                    scope.launch {
                        try {
                            controller.deposit(amount)
                        } catch (e: Exception) {
                            // Error already logged in controller
                        }
                    }
                },
                enabled = !inCS && !hasPendingRequest,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LogPanel(
                    entries = controller.eventLogger.getEntries(),
                    eventLogger = controller.eventLogger,
                    modifier = Modifier.weight(2f)
                )
                PeersList(
                    peers = connectedPeers,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
