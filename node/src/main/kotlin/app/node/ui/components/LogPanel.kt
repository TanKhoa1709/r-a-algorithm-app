package app.node.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.node.controller.EventLogEntry
import app.node.controller.EventLogger
import app.node.controller.EventType
import app.node.ui.theme.NodeColors

@Composable
fun LogPanel(
    entries: List<EventLogEntry>,
    eventLogger: EventLogger? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Auto-scroll to bottom when new entries arrive (newest events at bottom)
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            kotlinx.coroutines.delay(100) // Delay to ensure layout is ready
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = NodeColors.CardShadow,
                spotColor = NodeColors.CardShadow
            )
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, NodeColors.CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        backgroundColor = NodeColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header with icon and clear button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = NodeColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Event Log",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = NodeColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${entries.size} events",
                    style = MaterialTheme.typography.caption,
                    color = NodeColors.TextMuted,
                    modifier = Modifier.padding(end = 8.dp)
                )
                if (eventLogger != null && entries.isNotEmpty()) {
                    IconButton(
                        onClick = { eventLogger.clear() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear logs",
                            tint = NodeColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // Log entries container with scroll
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        NodeColors.SurfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (entries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = NodeColors.TextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No events yet",
                                    style = MaterialTheme.typography.body1,
                                    color = NodeColors.TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Events will appear here",
                                    style = MaterialTheme.typography.caption,
                                    color = NodeColors.TextMuted.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        // Display entries in chronological order (oldest first, newest at bottom)
                        entries.forEachIndexed { index, entry ->
                            val isLatest = index == entries.size - 1 // Last entry is the newest
                            LogEntry(entry, isLatest = isLatest)
                            if (index < entries.size - 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntry(entry: EventLogEntry, isLatest: Boolean = false) {
    val (backgroundColor, textColor, icon, borderColor) = when (entry.type) {
        EventType.SUCCESS -> Quadruple(
            NodeColors.SuccessLight.copy(alpha = 0.3f),
            NodeColors.Success,
            Icons.Default.CheckCircle,
            NodeColors.SuccessBorder.copy(alpha = 0.5f)
        )
        EventType.WARNING -> Quadruple(
            NodeColors.WarningLight.copy(alpha = 0.3f),
            NodeColors.Warning,
            Icons.Default.Warning,
            NodeColors.WarningBorder.copy(alpha = 0.5f)
        )
        EventType.ERROR -> Quadruple(
            NodeColors.ErrorLight.copy(alpha = 0.3f),
            NodeColors.Error,
            Icons.Default.Warning,
            NodeColors.ErrorBorder.copy(alpha = 0.5f)
        )
        EventType.REQUEST_SENT -> Quadruple(
            Color(0xFF2196F3).copy(alpha = 0.1f),
            Color(0xFF2196F3),
            Icons.Default.Info,
            Color(0xFF2196F3).copy(alpha = 0.3f)
        )
        EventType.REQUEST_RECEIVED -> Quadruple(
            Color(0xFF2196F3).copy(alpha = 0.08f),
            Color(0xFF1976D2),
            Icons.Default.Info,
            Color(0xFF2196F3).copy(alpha = 0.2f)
        )
        EventType.REPLY_SENT -> Quadruple(
            Color(0xFF9C27B0).copy(alpha = 0.1f),
            Color(0xFF9C27B0),
            Icons.Default.CheckCircle,
            Color(0xFF9C27B0).copy(alpha = 0.3f)
        )
        EventType.REPLY_RECEIVED -> Quadruple(
            Color(0xFF9C27B0).copy(alpha = 0.08f),
            Color(0xFF7B1FA2),
            Icons.Default.CheckCircle,
            Color(0xFF9C27B0).copy(alpha = 0.2f)
        )
        EventType.RELEASE_SENT -> Quadruple(
            Color(0xFF00BCD4).copy(alpha = 0.1f),
            Color(0xFF00BCD4),
            Icons.Default.ExitToApp,
            Color(0xFF00BCD4).copy(alpha = 0.3f)
        )
        EventType.RELEASE_RECEIVED -> Quadruple(
            Color(0xFF00BCD4).copy(alpha = 0.08f),
            Color(0xFF0097A7),
            Icons.Default.Info,
            Color(0xFF00BCD4).copy(alpha = 0.2f)
        )
        else -> Quadruple(
            NodeColors.SurfaceVariant.copy(alpha = 0.5f),
            NodeColors.TextPrimary,
            Icons.Default.Info,
            NodeColors.CardBorder.copy(alpha = 0.5f)
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .border(
                width = if (isLatest) 2.dp else 1.dp,
                color = if (isLatest) borderColor else borderColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entry.message,
                    style = MaterialTheme.typography.body2,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.formatTimestamp(),
                    style = MaterialTheme.typography.caption,
                    color = NodeColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            
            if (entry.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .background(
                            NodeColors.SurfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    entry.details.forEach { (key, value) ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "$key:",
                                style = MaterialTheme.typography.caption,
                                color = NodeColors.TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(80.dp)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.caption,
                                color = NodeColors.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper data class for quadruple
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
