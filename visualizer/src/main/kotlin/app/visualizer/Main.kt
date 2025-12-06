package app.visualizer

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun main() = application {
    val scope = remember {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // client giữ suốt vòng đời app
    val client = remember {
        VisualizerClient(scope)
    }

    var uiState by remember { mutableStateOf(VisualizerState()) }

    // collect StateFlow từ client
    LaunchedEffect(Unit) {
        client.state.collectLatest { newState ->
            uiState = newState
        }
    }

    // Kết nối tới cs-host
    LaunchedEffect(Unit) {
        client.connect("ws://localhost:8080/visualizer")
        // nếu cs-host chạy port khác, sửa lại 8080 cho đúng
    }

    Window(
        onCloseRequest = {
            client.close()
            exitApplication()
        }, title = "R&A CS Visualizer", state = WindowState(width = 1200.dp, height = 700.dp)
    ) {
        VisualizerScreen(uiState)
    }
}
