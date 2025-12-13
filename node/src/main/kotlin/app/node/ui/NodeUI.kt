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
import androidx.compose.ui.graphics.Color
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
fun NodeUI(
    controller: NodeController,
    currentCsHostUrl: String,
    onUpdateCsHostUrl: (String) -> Unit
) {
    var inCS by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf(0L) }
    var csState by remember { mutableStateOf<CSState?>(null) }
    val scope = rememberCoroutineScope()

    var hasPendingRequest by remember { mutableStateOf(false) }

    // State cho phần chỉnh CS Host
    var editing by remember { mutableStateOf(false) }
    var csHostUrlText by remember { mutableStateOf(currentCsHostUrl) }
    var showRestartHint by remember { mutableStateOf(false) }

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

            Spacer(modifier = Modifier.height(16.dp))

            // edit CS HOST url
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CS Host",
                        style = MaterialTheme.typography.caption,
                        color = NodeColors.TextMuted
                    )
                    Text(
                        text = csHostUrlText,
                        style = MaterialTheme.typography.body2,
                        color = NodeColors.TextPrimary
                    )
                    if (showRestartHint) {
                        Text(
                            text = "Close and reopen the application if you want to use the newly saved CS Host url.",
                            style = MaterialTheme.typography.caption,
                            color = Color(0xFFB00020)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        editing = !editing
                        showRestartHint = false
                        csHostUrlText = currentCsHostUrl
                    }
                ) {
                    Text(
                        text = if (editing) "Cancel" else "Edit CS Host url",
                        color = NodeColors.Primary
                    )
                }
            }

            if (editing) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = csHostUrlText,
                            onValueChange = { csHostUrlText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("CS Host URL") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    editing = false
                                    csHostUrlText = currentCsHostUrl
                                }
                            ) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val trimmed = csHostUrlText.trim()
                                    if (trimmed.isNotEmpty()) {
                                        onUpdateCsHostUrl(trimmed)
                                        showRestartHint = true
                                        editing = false
                                    }
                                },
                                enabled = csHostUrlText.isNotBlank(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                        // state update qua LaunchedEffect
                    } catch (_: Exception) {
                    }
                },
                onReleaseCS = {
                    try {
                        controller.releaseCriticalSection()
                    } catch (_: Exception) {
                    }
                },
                enabled = !inCS && !hasPendingRequest,
                releaseEnabled = inCS,
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
