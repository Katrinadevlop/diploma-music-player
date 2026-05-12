package ru.musikkk.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.musikkk.player.domain.library.Track
import ru.musikkk.player.ui.format.formatDuration
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

/**
 * Унифицированная строка трека для второстепенных экранов
 * (Liked / Playlist detail / Recent / Top / Search-результаты).
 * На основном Release Detail используется собственный TrackRow
 * с номером и без обложки — здесь обложка нужна, чтобы экран
 * читался без контекста релиза.
 */
@Composable
fun TrackListRow(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverId = track.coverId,
            contentDescription = track.title,
            modifier = Modifier.size(48.dp),
            fallbackText = track.title,
            radius = MusikkkRadius.sm,
        )
        Spacer(Modifier.width(MusikkkSpacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(track.artistName, track.albumName)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(MusikkkSpacing.s2))
        if (trailing != null) {
            trailing()
            Spacer(Modifier.width(MusikkkSpacing.s2))
        }
        Text(
            text = formatDuration(track.duration),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
