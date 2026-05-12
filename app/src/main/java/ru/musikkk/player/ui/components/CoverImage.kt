package ru.musikkk.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import ru.musikkk.player.core.network.MediaUrls
import ru.musikkk.player.ui.theme.MusikkkColors
import ru.musikkk.player.ui.theme.MusikkkRadius

/**
 * Обложка релиза/трека по `cover_blob_id`. Если id пуст — рисуем
 * плейсхолдер первой буквой имени.
 *
 * Авторизация на запросе обложки приходит автоматически через
 * `ImageLoader`, настроенный с `@AuthClient` OkHttp в [ImageLoaderModule].
 */
@Composable
fun CoverImage(
    coverId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackText: String? = null,
    radius: Dp = MusikkkRadius.md,
) {
    val shape = RoundedCornerShape(radius)
    val url = MediaUrls.coverUrl(coverId)

    Box(
        modifier = modifier
            .clip(shape)
            .background(MusikkkColors.SurfaceHover, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (!fallbackText.isNullOrBlank()) {
            Text(
                text = fallbackText.first().uppercaseChar().toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MusikkkColors.TextMuted,
            )
        }
    }
}
