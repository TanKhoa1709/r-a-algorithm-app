package app.node.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.node.ui.theme.NodeColors

@Composable
fun StatusBar(
    inCriticalSection: Boolean,
    clock: Long,
    csState: app.models.CSState?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        backgroundColor = NodeColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (inCriticalSection) NodeColors.InCSText.copy(alpha = 0.3f) else NodeColors.CardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    if (inCriticalSection) NodeColors.InCS.copy(alpha = 0.3f) else NodeColors.CardBackground,
                    RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NodeColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = NodeColors.TextPrimary
                )
            }

            // CS Status with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CS Status:",
                    style = MaterialTheme.typography.body1,
                    color = NodeColors.TextSecondary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (inCriticalSection) NodeColors.InCS else NodeColors.Idle)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (inCriticalSection) "IN CS" else "IDLE",
                        color = if (inCriticalSection) NodeColors.InCSText else NodeColors.IdleText,
                        style = MaterialTheme.typography.body2.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clock display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Clock:",
                    style = MaterialTheme.typography.body1,
                    color = NodeColors.TextSecondary
                )
                Text(
                    text = clock.toString(),
                    style = MaterialTheme.typography.body1.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = NodeColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = NodeColors.Divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            csState?.let { state ->
                // Host Locked status with icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (state.isLocked) NodeColors.Warning else NodeColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Host Locked:",
                            style = MaterialTheme.typography.body1,
                            color = NodeColors.TextSecondary
                        )
                    }
                    Text(
                        text = if (state.isLocked) "YES (${state.currentHolder ?: "?"})" else "NO",
                        style = MaterialTheme.typography.body1.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (state.isLocked) NodeColors.Warning else NodeColors.TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Queue display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Queue: ",
                        style = MaterialTheme.typography.body2,
                        color = NodeColors.TextSecondary
                    )
                    Text(
                        text = if (state.queue.isEmpty()) "[]" else state.queue.joinToString(", "),
                        style = MaterialTheme.typography.body2.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = NodeColors.TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Total accesses
                Text(
                    text = "Total accesses: ${state.totalAccesses}",
                    style = MaterialTheme.typography.body2,
                    color = NodeColors.TextSecondary
                )
                
                // Violations with warning style
                if (state.violations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NodeColors.ErrorLight)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NodeColors.Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Violations: ${state.violations.size}",
                            color = NodeColors.Error,
                            style = MaterialTheme.typography.body2.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

