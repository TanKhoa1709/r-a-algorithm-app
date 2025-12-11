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
import app.models.CSState
import kotlinx.coroutines.launch

/**
 * Main UI for the node application
 */
@Composable
fun NodeUI(controller: NodeController) {
    var inCS by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf(0L) }
    var csState by remember { mutableStateOf<CSState?>(null) }
    val scope = rememberCoroutineScope()
    
    var hasPendingRequest by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Seed state via HTTP API once
        scope.launch {
            runCatching { controller.refreshCsHostState() }
        }
        while (true) {
            clock = controller.getClock()
            inCS = controller.isInCriticalSection()
            hasPendingRequest = controller.hasPendingRequest()
            csState = controller.getCsHostState()
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
                        text = "Ricart-Agrawala Node",
                        style = MaterialTheme.typography.h4.copy(
                            fontSize = 26.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = NodeColors.TextPrimary
                    )
                    Text(
                        text = "Distributed Mutual Exclusion",
                        style = MaterialTheme.typography.caption,
                        color = NodeColors.TextMuted
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StatusBar(
                    inCriticalSection = inCS,
                    clock = clock,
                    csState = csState,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            ControlPanel(
                onRequestCS = {
                    try {
                        controller.requestCriticalSection()
                        // State will be updated by LaunchedEffect
                    } catch (e: Exception) {
                        // Handle error
                    }
                },
                onReleaseCS = {
                    try {
                        controller.releaseCriticalSection()
                        // State will be updated by LaunchedEffect
                    } catch (e: Exception) {
                        // Handle error
                    }
                },
                enabled = !inCS && !hasPendingRequest,
                releaseEnabled = inCS,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LogPanel(
                    modifier = Modifier.weight(1f)
                )
                PeersList(
                    peers = controller.getConnectedNodes(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
