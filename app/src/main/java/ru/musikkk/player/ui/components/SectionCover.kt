package ru.musikkk.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.musikkk.player.ui.theme.MusikkkColors
import ru.musikkk.player.ui.theme.MusikkkRadius

/**
 * «Иконическая» обложка для встроенных секций (Liked, Playlists, Recent,
 * Top, Upload) — повторяет паттерн веб-клиента (`likes_default.svg`,
 * `playlist_default.svg` и т.д.): большая иконка по центру тёмной
 * заливки с лёгким мятным градиентом.
 */
@Composable
fun SectionCover(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    radius: androidx.compose.ui.unit.Dp = MusikkkRadius.lg,
) {
    val shape = RoundedCornerShape(radius)
    val gradient = Brush.linearGradient(
        listOf(
            MusikkkColors.SurfaceElevated,
            MusikkkColors.AccentBgSelected,
        ),
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient, shape)
            .border(1.dp, MusikkkColors.BorderMuted, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize(0.45f),
        )
    }
}
