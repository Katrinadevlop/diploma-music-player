package ru.musikkk.player.data.user

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.musikkk.player.core.network.api.UserDataApi
import ru.musikkk.player.core.network.dto.BumpPlaycountRequest
import ru.musikkk.player.domain.user.PlaycountEntry

@Singleton
class PlaycountsRepository @Inject constructor(
    private val api: UserDataApi,
) {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    suspend fun refresh() {
        val resp = api.getPlaycounts()
        _counts.value = resp.counts
    }

    /** Бампит счётчик на сервере на +1 и обновляет локальный кэш. */
    suspend fun recordPlayed(trackPath: String) {
        val resp = api.bumpPlaycount(BumpPlaycountRequest(trackId = trackPath, by = 1))
        _counts.update { current -> current + (trackPath to resp.count) }
    }

    /** Снимок «топа» — для UI-экрана. */
    fun topSnapshot(limit: Int = 100): List<PlaycountEntry> =
        _counts.value.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { PlaycountEntry(it.key, it.value) }
}
