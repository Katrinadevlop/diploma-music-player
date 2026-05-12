package ru.musikkk.player.ui.theme

import androidx.compose.ui.unit.dp

// Шкала отступов / радиусов / blur'а — копия токенов из веб-клиента.
object MusikkkSpacing {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 24.dp
    val s6 = 32.dp
    val s7 = 48.dp
    val s8 = 64.dp
}

object MusikkkRadius {
    val sm = 10.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val pill = 999.dp
}

object MusikkkBlur {
    val regular = 16.dp
    val strong = 26.dp

    // На сайте размытие фоновой обложки — 48px, повторяем те же 48dp
    // для фонового слоя приложения.
    val backgroundCover = 48.dp
}
