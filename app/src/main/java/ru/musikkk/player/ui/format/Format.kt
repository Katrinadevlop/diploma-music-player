package ru.musikkk.player.ui.format

import java.util.Locale

/** Форматирует длительность трека из секунд в `M:SS` (или `H:MM:SS`). */
fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

/**
 * Возвращает короткое представление даты релиза (только год), если оно
 * парсится — иначе исходную строку.
 */
fun formatReleaseYear(date: String?, year: String?): String? {
    if (!year.isNullOrBlank()) return year
    val d = date?.takeIf { it.isNotBlank() } ?: return null
    // Сервер обычно отдаёт ISO-подобную строку «2023-08-12»; берём первые 4.
    return d.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: d
}
