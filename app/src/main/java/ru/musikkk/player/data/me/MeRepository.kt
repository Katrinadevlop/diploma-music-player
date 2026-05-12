package ru.musikkk.player.data.me

import ru.musikkk.player.domain.me.Me

interface MeRepository {
    /** Загружает профиль текущего пользователя через `GET /api/me`. */
    suspend fun fetchMe(): Me
}
