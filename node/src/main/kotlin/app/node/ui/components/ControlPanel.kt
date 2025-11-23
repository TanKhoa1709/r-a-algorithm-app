package app.node.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ui.components.PrimaryButton
import app.ui.components.SecondaryButton

@Composable
fun ControlPanel(
    onRequestCS: () -> Unit,
    onReleaseCS: () -> Unit,
    enabled: Boolean,
    releaseEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Control Panel",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

