package app.node.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.node.controller.NodeController
import app.node.ui.components.*
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
    
    LaunchedEffect(Unit) {
        // Seed state via HTTP API once
        scope.launch {
            runCatching { controller.refreshCsHostState() }
        }
        while (true) {
            clock = controller.getClock()
            inCS = controller.isInCriticalSection()
            csState = controller.getCsHostState()
            kotlinx.coroutines.delay(100)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Ricart-Agrawala Node",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusBar(
                inCriticalSection = inCS,
                clock = clock,
                csState = csState,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ControlPanel(
            onRequestCS = {
                try {
                    controller.requestCriticalSection()
                    inCS = true
                } catch (e: Exception) {
                    // Handle error
                }
            },
            onReleaseCS = {
                try {
                    controller.releaseCriticalSection()
                    inCS = false
                } catch (e: Exception) {
                    // Handle error
                }
            },
            enabled = !inCS,
            releaseEnabled = inCS,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

