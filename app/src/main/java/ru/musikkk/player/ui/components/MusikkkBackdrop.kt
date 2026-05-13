package ru.musikkk.player.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.musikkk.player.core.network.MediaUrls
import ru.musikkk.player.ui.theme.MusikkkBlur

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
            // Градиент-fallback — без обложки заполняем экран мягким
            // переходом от приподнятой поверхности к основному фону.
            // Через theme-токены работает и в светлой теме.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            )
        }
        // Тинт поверх — чтобы любой UI читался на любой обложке.
        // Цвет берём из темы (background) — в тёмной получится почти
        // чёрный overlay, в светлой — почти белый, текст в обеих
        // темах остаётся контрастным.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
        )
    }
}
