package app.node.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.node.controller.NodeController
import app.ui.components.*
import app.ui.theme.CommonTheme
import app.node.ui.components.*

/**
 * Main UI for the node application
 */
@Composable
fun NodeUI(controller: NodeController) {
    var inCS by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            clock = controller.getClock()
            inCS = controller.isInCriticalSection()
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

