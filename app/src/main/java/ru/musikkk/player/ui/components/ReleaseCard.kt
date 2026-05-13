package ru.musikkk.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

/**
 * Карточка релиза в grid'е: квадратная обложка → название → имя артиста.
 * Совпадает по структуре с `<div class="card">` на веб-клиенте (см.
 * `static/styles.css` → `.grid-releases .card`). Используется на главной,
 * экране артиста и в результатах поиска.
 */
@Composable
fun ReleaseCard(
    release: Release,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtistName: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        CoverImage(
            coverId = release.coverId,
            contentDescription = release.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            fallbackText = release.name,
            radius = MusikkkRadius.md,
        )
        Spacer(Modifier.height(MusikkkSpacing.s2))
        Text(
            text = release.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showArtistName) {
            Text(
                text = release.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
