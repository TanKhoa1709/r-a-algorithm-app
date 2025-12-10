package app.node.ui.theme

import androidx.compose.ui.graphics.Color

object NodeColors {
    // Primary palette - Vibrant indigo/violet gradient
    val Primary = Color(0xFF6366F1)
    val PrimaryLight = Color(0xFF818CF8)
    val PrimaryDark = Color(0xFF4F46E5)
    val PrimaryGradientStart = Color(0xFF6366F1)
    val PrimaryGradientEnd = Color(0xFF8B5CF6)
    
    // Secondary palette - Teal accent
    val Secondary = Color(0xFF14B8A6)
    val SecondaryLight = Color(0xFF2DD4BF)
    val SecondaryDark = Color(0xFF0D9488)
    
    // Accent colors for visual pop
    val Accent1 = Color(0xFFF472B6) // Pink
    val Accent2 = Color(0xFFFBBF24) // Amber
    val Accent3 = Color(0xFF34D399) // Emerald
    
    // Status colors
    val Success = Color(0xFF10B981)
    val SuccessLight = Color(0xFFD1FAE5)
    val SuccessBorder = Color(0xFF6EE7B7)
    val Warning = Color(0xFFF59E0B)
    val WarningLight = Color(0xFFFEF3C7)
    val WarningBorder = Color(0xFFFCD34D)
    val Error = Color(0xFFEF4444)
    val ErrorLight = Color(0xFFFEE2E2)
    val ErrorBorder = Color(0xFFFCA5A5)
    
    // Critical Section states - More vibrant
    val InCS = Color(0xFFEDE9FE)       // Lighter violet background
    val InCSText = Color(0xFF7C3AED)   // Purple text
    val InCSBorder = Color(0xFFC4B5FD) // Violet border
    val Idle = Color(0xFFD1FAE5)       // Light green background
    val IdleText = Color(0xFF059669)   // Green text
    val IdleBorder = Color(0xFF6EE7B7) // Green border
    val Wanted = Color(0xFFFEF3C7)     // Light amber
    val WantedText = Color(0xFFD97706) // Amber text
    val WantedBorder = Color(0xFFFCD34D) // Amber border
    
    // Surface colors - Premium light theme with depth
    val Background = Color(0xFFF8FAFC)
    val BackgroundGradientStart = Color(0xFFF1F5F9)
    val BackgroundGradientEnd = Color(0xFFE2E8F0)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)
    val SurfaceElevated = Color(0xFFFFFFFF)
    val CardBackground = Color(0xFFFFFFFF)
    val CardBackgroundHover = Color(0xFFFAFAFA)
    val CardBorder = Color(0xFFE2E8F0)
    val CardShadow = Color(0x1A6366F1) // Primary with alpha for shadow
    
    // Glass effect colors
    val GlassBackground = Color(0xB3FFFFFF) // White with 70% opacity
    val GlassBorder = Color(0x33FFFFFF)     // White with 20% opacity
    
    // Text colors
    val TextPrimary = Color(0xFF1E293B)
    val TextSecondary = Color(0xFF64748B)
    val TextMuted = Color(0xFF94A3B8)
    val TextOnPrimary = Color(0xFFFFFFFF)
    
    // Divider and borders
    val Divider = Color(0xFFE2E8F0)
    val DividerLight = Color(0xFFF1F5F9)
    val Border = Color(0xFFCBD5E1)
    val BorderFocus = Color(0xFF6366F1)
    
    // Connected peer indicator
    val Connected = Color(0xFF10B981)
    val ConnectedGlow = Color(0x3310B981) // With alpha for glow effect
    val Disconnected = Color(0xFF94A3B8)
    
    // Button specific colors
    val ButtonPrimaryHover = Color(0xFF4F46E5)
    val ButtonSecondaryHover = Color(0xFFE2E8F0)
}
