package ru.musikkk.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import ru.musikkk.player.ui.theme.MusikkkColors
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

/**
 * Универсальная карточка для главной: квадратная обложка → название →
 * подзаголовок. Соответствует `tileCard()` в `static/app/library.js`
 * (см. вызовы из `renderSmartHome` и `renderPlaylistsHome`).
 *
 * Может рендерить либо обложку по `coverId`, либо иконку (для тайлов
 * вроде «Создать плейлист»). Если ни того ни другого нет — рисуем
 * заглушку с первой буквой заголовка.
 */
@Composable
fun HomeTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverId: String? = null,
    fallbackIcon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        if (fallbackIcon != null && coverId == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(MusikkkRadius.md)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(MusikkkRadius.md)),
                ) {
                    // Фон карточки тот же что у CoverImage fallback'а — чтобы
                    // тайлы с обложкой и без неё визуально не «прыгали».
                }
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = MusikkkColors.TextMuted,
                    modifier = Modifier.size(56.dp),
                )
            }
        } else {
            CoverImage(
                coverId = coverId,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                fallbackText = title,
                radius = MusikkkRadius.md,
            )
        }
        Spacer(Modifier.height(MusikkkSpacing.s2))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
