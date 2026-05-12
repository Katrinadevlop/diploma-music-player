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
import ru.musikkk.player.data.playback.PlaybackController

/**
 * Сводит воспроизведение и пользовательские данные:
 * - при смене текущего трека пишет в Recent и бампит Playcount,
 * - раз в [CONTINUE_SAVE_INTERVAL_MS] сохраняет позицию в Continue.
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
    private val trackPathResolver: BlobToPathResolver,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trackingJob: Job? = null

    fun start() {
        if (trackingJob?.isActive == true) return
        trackingJob = scope.launch {
            // Прогреваем серверный кэш — likes/playlists/continue нужны
            // на других экранах сразу, без задержки на первый refresh().
            launch { runCatching { likesRepository.refresh() } }
            launch { runCatching { playlistsRepository.refresh() } }
            launch { runCatching { continueRepository.refresh() } }
            launch { observeTrackChanges() }
            launch { saveContinuePeriodically() }
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
