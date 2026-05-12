package ru.musikkk.player.domain.me

/** Минимальный профиль пользователя — то, что приходит из `GET /api/me`. */
data class Me(
    val publicId: String,
    val username: String,
)
