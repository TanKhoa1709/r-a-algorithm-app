package app.node.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.node.ui.theme.NodeColors

@Composable
fun ControlPanel(
    onRequestCS: () -> Unit,
    onReleaseCS: () -> Unit,
    enabled: Boolean,
    releaseEnabled: Boolean,
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
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NodeColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Control Panel",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = NodeColors.TextPrimary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    text = "Request CS",
                    onClick = onRequestCS,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
                
                SecondaryButton(
                    text = "Release CS",
                    onClick = onReleaseCS,
                    enabled = releaseEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
