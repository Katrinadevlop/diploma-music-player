package ru.musikkk.player.data.user

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.musikkk.player.core.network.api.UserDataApi
import ru.musikkk.player.core.network.dto.AddRecentRequest
import ru.musikkk.player.domain.user.RecentEntry

@Singleton
class RecentRepository @Inject constructor(
    private val api: UserDataApi,
) {
    private val _recent = MutableStateFlow<List<RecentEntry>>(emptyList())
    val recent: StateFlow<List<RecentEntry>> = _recent.asStateFlow()

    suspend fun refresh() {
        val resp = api.getRecent()
        _recent.value = resp.recent.map { RecentEntry(trackPath = it.trackId, atMs = it.at) }
    }

    /** Серверу — POST /api/user/recent; локальный кэш не трогаем, пусть `refresh` подтянет. */
    suspend fun recordPlayed(trackPath: String) {
        api.addRecent(AddRecentRequest(trackId = trackPath, at = System.currentTimeMillis()))
    }
}
