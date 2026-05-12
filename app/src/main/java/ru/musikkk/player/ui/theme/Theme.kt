package ru.musikkk.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ru.musikkk.player.domain.settings.ThemeMode

private val DarkScheme = darkColorScheme(
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

private val LightScheme = lightColorScheme(
    primary = MusikkkLightColors.Accent,
    onPrimary = Color.White,
    primaryContainer = MusikkkLightColors.AccentBg,
    onPrimaryContainer = MusikkkLightColors.AccentText,
    secondary = MusikkkLightColors.Accent,
    onSecondary = Color.White,
    background = MusikkkLightColors.Background,
    onBackground = MusikkkLightColors.TextPrimary,
    surface = MusikkkLightColors.Surface,
    onSurface = MusikkkLightColors.TextPrimary,
    surfaceVariant = MusikkkLightColors.SurfaceHover,
    onSurfaceVariant = MusikkkLightColors.TextSecondary,
    outline = MusikkkLightColors.BorderStrong,
    outlineVariant = MusikkkLightColors.BorderMuted,
    error = MusikkkLightColors.Danger,
    onError = Color.White,
)

/**
 * Решает, тёмная сейчас тема или светлая, с учётом пользовательской
 * настройки и системной темы. Возвращает `true`, если показывать
 * тёмное оформление.
 */
@Composable
fun isDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun MusikkkTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = isDarkTheme(themeMode)
    val scheme = if (dark) DarkScheme else LightScheme

    MaterialTheme(
        colorScheme = scheme,
        typography = MusikkkTypography,
        shapes = MusikkkShapes,
        content = content,
    )
}
