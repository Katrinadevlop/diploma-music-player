package ru.musikkk.player.data.user

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.data.playback.PlaybackController

/**
 * Сводит воспроизведение и пользовательские данные:
 * - при смене текущего трека пишет в Recent и бампит Playcount,
 * - раз в [CONTINUE_SAVE_INTERVAL_MS] сохраняет позицию в Continue,
 * - на появление токена (логин/рестарт с активной сессией) подтягивает
 *   серверный кэш Likes/Playlists/Continue, чтобы UI на других экранах
 *   сразу видел актуальные данные.
 *
 * Стартует один раз при первом обращении (через `start()` из
 * `MusikkkApp.onCreate`). Всё через `runCatching`, чтобы офлайн не
 * крэшил воспроизведение.
 */
@Singleton
class PlaybackTracker @Inject constructor(
    private val playbackController: PlaybackController,
    private val recentRepository: RecentRepository,
    private val playcountsRepository: PlaycountsRepository,
    private val continueRepository: ContinueRepository,
    private val likesRepository: LikesRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val authRepository: AuthRepository,
    private val trackPathResolver: BlobToPathResolver,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trackingJob: Job? = null

    fun start() {
        if (trackingJob?.isActive == true) return
        trackingJob = scope.launch {
            launch { hydrateOnLogin() }
            launch { observeTrackChanges() }
            launch { saveContinuePeriodically() }
        }
    }

    /**
     * Слушает токен — при появлении не-null значения (свежий логин или
     * рестарт приложения с сохранённой сессией) подтягивает серверный
     * кэш пользовательских данных.
     */
    private suspend fun hydrateOnLogin() {
        authRepository.tokenFlow
            .map { it != null }
            .distinctUntilChanged()
            .collect { authed ->
                if (!authed) return@collect
                // `.collect { }` не CoroutineScope — пускаем загрузку в
                // фоновых корутинах через класс-скоуп. Все три параллельно,
                // ошибки глотаем — heavy lift не критичен для playback.
                scope.launch { runCatching { likesRepository.refresh() } }
                scope.launch { runCatching { playlistsRepository.refresh() } }
                scope.launch { runCatching { continueRepository.refresh() } }
            }
    }

    private suspend fun observeTrackChanges() {
        playbackController.state
            .map { it.currentTrack?.blobId }
            .distinctUntilChanged()
            .collect { blobId ->
                val path = blobId?.let { trackPathResolver.resolve(it) } ?: return@collect
                runCatching { recentRepository.recordPlayed(path) }
                runCatching { playcountsRepository.recordPlayed(path) }
            }
    }

    private suspend fun saveContinuePeriodically() {
        while (true) {
            delay(CONTINUE_SAVE_INTERVAL_MS)
            val snapshot = playbackController.state.value
            val blob = snapshot.currentTrack?.blobId
            if (blob != null && snapshot.isPlaying) {
                val path = trackPathResolver.resolve(blob) ?: continue
                val seconds = snapshot.positionMs / 1000.0
                runCatching { continueRepository.save(path, seconds) }
            }
        }
    }

    private companion object {
        const val CONTINUE_SAVE_INTERVAL_MS = 5_000L
    }
}
