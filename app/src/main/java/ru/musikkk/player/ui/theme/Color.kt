package ru.musikkk.player.ui.theme

import androidx.compose.ui.graphics.Color

// Дизайн-токены — копия `:root` из веб-клиента (`static/styles.css`).
// Названия максимально близкие к CSS-переменным, чтобы при сверке с вебом
// легко было найти соответствие.

internal object MusikkkColors {
    val Background = Color(0xFF000000)

    val Surface = Color(0x8F141414)         // rgba(20,20,20,0.56)
    val SurfaceHover = Color(0x9E1C1C1C)    // rgba(28,28,28,0.62)
    val SurfaceElevated = Color(0xDB0F0F0F) // rgba(15,15,15,0.86)

    val BorderMuted = Color(0x1AFFFFFF)     // rgba(255,255,255,0.10)
    val BorderStrong = Color(0x29FFFFFF)    // rgba(255,255,255,0.16)

    val TextPrimary = Color(0xEBFFFFFF)     // rgba(255,255,255,0.92)
    val TextSecondary = Color(0xB3FFFFFF)   // rgba(255,255,255,0.70)
    val TextMuted = Color(0x8CFFFFFF)       // rgba(255,255,255,0.55)

    val Accent = Color(0xFF5DE8B8)          // rgba(93,232,184,1)
    val AccentText = Color(0xF5DCFFF3)      // rgba(220,255,243,0.96)
    val AccentBorder = Color(0x805DE8B8)
    val AccentBg = Color(0x245DE8B8)
    val AccentBgHover = Color(0x335DE8B8)
    val AccentBgSelected = Color(0x0F5DE8B8)

    val Danger = Color(0xFFFF5555)
    val DangerText = Color(0xF2FFAAAA)
    val DangerBg = Color(0x0FFF5555)
    val DangerBorder = Color(0x2EFF5555)

    val Success = Accent
    val Warning = Color(0xFFFFC35A)
}
