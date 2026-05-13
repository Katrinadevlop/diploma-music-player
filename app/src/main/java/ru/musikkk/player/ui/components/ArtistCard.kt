package ru.musikkk.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.musikkk.player.domain.library.Artist
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

/**
 * Карточка артиста в grid'е: круглый аватар → имя по центру.
 * Соответствует `.artist-card` в `static/styles.css` веб-клиента —
 * круглая обложка 160×160 в центре, под ней имя.
 */
@Composable
fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverImage(
            coverId = artist.avatarCoverId,
            contentDescription = artist.name,
            modifier = Modifier.size(140.dp),
            fallbackText = artist.name,
            // pill-радиус из дизайн-токенов даёт круг при равных сторонах
            radius = MusikkkRadius.pill,
        )
        Spacer(Modifier.height(MusikkkSpacing.s3))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
