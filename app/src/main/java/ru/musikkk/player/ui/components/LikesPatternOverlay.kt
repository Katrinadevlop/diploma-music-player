package ru.musikkk.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Точечный «обои» эффект для экрана Liked — повторяет `likes_bg.svg`
 * из веб-клиента: едва различимые белые точки на тёмной заливке.
 * Накладывается ПОВЕРХ обычного backdrop'a (или вместо него).
 *
 * Размер плитки 120×120, рисунок повторяется до краёв экрана.
 */
@Composable
fun LikesPatternOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tile = 120f
            val dots = listOf(
                Triple(18f, 26f, 1.6f) to 0.10f,
                Triple(62f, 14f, 1.2f) to 0.08f,
                Triple(94f, 52f, 1.8f) to 0.07f,
                Triple(34f, 92f, 1.3f) to 0.06f,
                Triple(104f, 104f, 1.4f) to 0.05f,
            )
            var y = 0f
            while (y < size.height) {
                var x = 0f
                while (x < size.width) {
                    for ((p, alpha) in dots) {
                        val (dx, dy, r) = p
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = r,
                            center = Offset(x + dx, y + dy),
                        )
                    }
                    x += tile
                }
                y += tile
            }
        }
    }
}
