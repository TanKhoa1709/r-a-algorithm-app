package app.node.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatusBar(
    inCriticalSection: Boolean,
    clock: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (inCriticalSection) Color(0xFFFFE0B2) else Color.White
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CS Status:")
                Text(
                    text = if (inCriticalSection) "IN CS" else "IDLE",
                    color = if (inCriticalSection) Color(0xFFD32F2F) else Color(0xFF388E3C),
                    style = MaterialTheme.typography.body1.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Clock:")
                Text(
                    text = clock.toString(),
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}
