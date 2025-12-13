package app.node.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.node.ui.theme.NodeColors

@Composable
fun ControlPanel(
    onWithdraw: (Long) -> Unit,
    onDeposit: (Long) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var withdrawAmount by remember { mutableStateOf("") }
    var depositAmount by remember { mutableStateOf("") }
    
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
                    text = "Bank Transaction",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = NodeColors.TextPrimary
                )
            }
            
            // Withdraw section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Withdraw",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium,
                    color = NodeColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it.filter { char -> char.isDigit() } },
                        label = { Text("Amount") },
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    PrimaryButton(
                        text = "Withdraw",
                        onClick = {
                            val amount = withdrawAmount.toLongOrNull() ?: 0L
                            if (amount > 0) {
                                onWithdraw(amount)
                                withdrawAmount = ""
                            }
                        },
                        enabled = enabled && withdrawAmount.toLongOrNull()?.let { it > 0 } == true,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
            
            // Deposit section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Deposit",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium,
                    color = NodeColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = depositAmount,
                        onValueChange = { depositAmount = it.filter { char -> char.isDigit() } },
                        label = { Text("Amount") },
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    SecondaryButton(
                        text = "Deposit",
                        onClick = {
                            val amount = depositAmount.toLongOrNull() ?: 0L
                            if (amount > 0) {
                                onDeposit(amount)
                                depositAmount = ""
                            }
                        },
                        enabled = enabled && depositAmount.toLongOrNull()?.let { it > 0 } == true,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}
