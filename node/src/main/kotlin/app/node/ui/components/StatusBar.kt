package app.node.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
    modifier: Modifier = Modifier
) {
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
                .fillMaxWidth()
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
                    "Lamport Clock:",
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
        }
    }
}

