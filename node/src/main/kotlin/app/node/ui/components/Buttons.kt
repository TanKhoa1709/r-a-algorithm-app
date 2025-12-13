package app.node.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.node.ui.theme.NodeColors

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = shape,
                ambientColor = NodeColors.Primary.copy(alpha = 0.3f),
                spotColor = NodeColors.Primary.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(
                brush = if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            NodeColors.PrimaryGradientStart,
                            NodeColors.PrimaryGradientEnd
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            NodeColors.Primary.copy(alpha = 0.4f),
                            NodeColors.PrimaryLight.copy(alpha = 0.4f)
                        )
                    )
                }
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(color = Color.White),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.button,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .shadow(
                elevation = if (enabled) 4.dp else 0.dp,
                shape = shape,
                ambientColor = NodeColors.CardShadow,
                spotColor = NodeColors.CardShadow
            )
            .clip(shape)
            .background(NodeColors.CardBackground)
            .border(
                width = 2.dp,
                color = if (enabled) NodeColors.Border else NodeColors.Border.copy(alpha = 0.5f),
                shape = shape
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(color = NodeColors.Primary),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.button,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) NodeColors.TextPrimary else NodeColors.TextMuted
        )
    }
}
