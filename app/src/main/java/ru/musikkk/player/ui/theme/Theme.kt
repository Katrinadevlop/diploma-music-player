package ru.musikkk.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Веб-клиент сделан только в тёмной теме («glass on black»), поэтому здесь
// держим единственную dark-схему как источник истины. Где Material3 не даёт
// нужной семантики (полупрозрачные «стеклянные» панели, blur и т. п.) —
// берём значения напрямую из MusikkkColors / MusikkkSpacing / MusikkkRadius.
//
// Цветами статус-бара и нав-бара управляет `enableEdgeToEdge()` из
// MainActivity — ручные statusBarColor/navigationBarColor задепрекейчены
// в API 35.
private val MusikkkColorScheme = darkColorScheme(
    primary = MusikkkColors.Accent,
    onPrimary = Color.Black,
    primaryContainer = MusikkkColors.AccentBg,
    onPrimaryContainer = MusikkkColors.AccentText,
    secondary = MusikkkColors.Accent,
    onSecondary = Color.Black,
    background = MusikkkColors.Background,
    onBackground = MusikkkColors.TextPrimary,
    surface = MusikkkColors.Surface,
    onSurface = MusikkkColors.TextPrimary,
    surfaceVariant = MusikkkColors.SurfaceHover,
    onSurfaceVariant = MusikkkColors.TextSecondary,
    outline = MusikkkColors.BorderStrong,
    outlineVariant = MusikkkColors.BorderMuted,
    error = MusikkkColors.Danger,
    onError = Color.Black,
)

@Composable
fun MusikkkTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MusikkkColorScheme,
        typography = MusikkkTypography,
        shapes = MusikkkShapes,
        content = content,
    )
}
