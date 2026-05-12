package ru.musikkk.player.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.musikkk.player.core.network.MediaUrls
import ru.musikkk.player.ui.theme.MusikkkBlur
import ru.musikkk.player.ui.theme.MusikkkColors

/**
 * Полноэкранный фоновой слой «как на вебе»: размытая обложка трека/релиза
 * + тёмный тинт, чтобы текст контента не «плыл» по картинке.
 *
 * `Modifier.blur` на Android < 31 — no-op (ОС не умеет render-effect blur),
 * на этих устройствах увидим просто затемнённую обложку без размытия. Это
 * считаем приемлемым fallback'ом.
 *
 * Если `coverId` == null, рисуем вертикальный градиент с акцентом, чтобы
 * экран не выглядел плоско-чёрным.
 */
@Composable
fun MusikkkBackdrop(
    coverId: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (coverId != null) {
            val url = MediaUrls.coverUrl(coverId)
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MusikkkBlur.backgroundCover else 0.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            // Тёмный градиент с тёплым центром — мягкий fallback.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MusikkkColors.SurfaceElevated,
                                MusikkkColors.Background,
                            ),
                        ),
                    ),
            )
        }
        // Тинт поверх — чтобы любой UI читался на любой обложке.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
        )
    }
}
