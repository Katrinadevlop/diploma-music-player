package ru.musikkk.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.musikkk.player.ui.theme.MusikkkColors
import ru.musikkk.player.ui.theme.MusikkkRadius

/**
 * «Стеклянная» панель — полупрозрачный surface + тонкий бордер.
 * Аналог `.glass` классов из веб-клиента.
 *
 * Backdrop-blur (`Modifier.blur`) поддерживается только с API 31+,
 * поэтому пока берём только полупрозрачность + бордер. Реальный blur
 * добавим под `if (Build.VERSION.SDK_INT >= 31)` отдельным шагом.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    radius: Dp = MusikkkRadius.lg,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MusikkkColors.Surface, shape)
            .border(BorderStroke(1.dp, MusikkkColors.BorderMuted), shape),
    ) {
        content()
    }
}
