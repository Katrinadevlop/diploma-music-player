package ru.musikkk.player.data.user

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.musikkk.player.core.network.api.UserDataApi
import ru.musikkk.player.core.network.dto.ToggleLikeRequest

/**
 * Лайки храним только в памяти — сервер всегда источник истины, но
 * UI-«сердечки» на каждой строке трека должны рендериться синхронно,
 * поэтому держим `StateFlow<Set<rel_path>>` для быстрых `isLiked()`.
 *
 * `toggle()` — оптимистично переключаем локально и параллельно дёргаем
 * сервер; на ошибке откатываем.
 */
@Singleton
class LikesRepository @Inject constructor(
    private val api: UserDataApi,
) {
    private val _liked = MutableStateFlow<Set<String>>(emptySet())
    val liked: StateFlow<Set<String>> = _liked.asStateFlow()

    suspend fun refresh() {
        val resp = api.getLikes()
        _liked.value = resp.likes.toSet()
    }

    /**
     * Переключает лайк трека. Возвращает финальное состояние от сервера
     * (на случай гонки с другим клиентом).
     */
    suspend fun toggle(trackPath: String): Boolean {
        val wasLiked = trackPath in _liked.value
        // Optimistic update.
        _liked.update { current ->
            if (wasLiked) current - trackPath else current + trackPath
        }

        return try {
            val resp = api.toggleLike(ToggleLikeRequest(trackId = trackPath))
            _liked.update { current ->
                if (resp.liked) current + trackPath else current - trackPath
            }
            resp.liked
        } catch (e: Throwable) {
            // Rollback оптимистичного апдейта.
            _liked.update { current ->
                if (wasLiked) current + trackPath else current - trackPath
            }
            throw e
        }
    }

    fun isLiked(trackPath: String): Boolean = trackPath in _liked.value
}
