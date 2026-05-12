package ru.musikkk.player.data.library

import kotlinx.coroutines.flow.Flow
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.Track

interface LibraryRepository {

    /** Поток релизов из локального кэша. UI наблюдает за ним напрямую. */
    fun observeReleases(): Flow<List<Release>>

    /** Один релиз — для шапки экрана деталей. */
    fun observeRelease(releaseId: String): Flow<Release?>

    /** Треки конкретного релиза, упорядоченные. */
    fun observeReleaseTracks(releaseId: String): Flow<List<Track>>

    /**
     * Тянет свежие данные с сервера и переписывает локальный кэш.
     * Бросает исключение наверх — пусть ViewModel решает, как показать
     * пользователю ошибку (offline → данные останутся из кэша).
     */
    suspend fun refresh()
}
