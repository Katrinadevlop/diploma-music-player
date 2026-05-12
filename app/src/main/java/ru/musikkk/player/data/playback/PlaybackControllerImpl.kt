package ru.musikkk.player.data.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.musikkk.player.core.media.MusikkkPlaybackService
import ru.musikkk.player.domain.playback.PlayableTrack
import ru.musikkk.player.domain.playback.PlaybackState
import ru.musikkk.player.domain.playback.RepeatMode

/**
 * Тонкая обёртка над `MediaController`, прибитым к [MusikkkPlaybackService].
 *
 * Подключение к сервису ленивое: первая операция (например, [playQueue])
 * запускает асинхронную сборку контроллера, далее все вызовы идут через
 * него же. Подписка на `Player.Listener` пересобирает текущее состояние
 * в [_state]. Позиция трека «тикает» отдельной корутиной раз в полсекунды.
 */
@Singleton
class PlaybackControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlaybackController {

    // MediaController обязан жить на main thread, поэтому держим main-scope.
    private val mainScope = MainScope()

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    @Volatile
    private var controller: MediaController? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null

    private var positionJob: Job? = null

    override fun playQueue(queue: List<PlayableTrack>, startIndex: Int) {
        if (queue.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, queue.lastIndex)

        mainScope.launch {
            val ctrl = awaitController()
            val items = queue.map { it.toMediaItem() }
            ctrl.setMediaItems(items, safeIndex, /* startPositionMs = */ 0L)
            ctrl.prepare()
            ctrl.play()
            // Сразу обновим очередь в state — listener придёт чуть позже.
            _state.update {
                it.copy(queue = queue, currentIndex = safeIndex)
            }
        }
    }

    override fun togglePlayPause() {
        mainScope.launch {
            val ctrl = awaitController()
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
        }
    }

    override fun seekToMs(positionMs: Long) {
        mainScope.launch {
            awaitController().seekTo(positionMs.coerceAtLeast(0))
        }
    }

    override fun skipToNext() {
        mainScope.launch {
            val ctrl = awaitController()
            if (ctrl.hasNextMediaItem()) ctrl.seekToNext()
        }
    }

    override fun skipToPrevious() {
        mainScope.launch {
            val ctrl = awaitController()
            if (ctrl.hasPreviousMediaItem()) ctrl.seekToPrevious() else ctrl.seekTo(0)
        }
    }

    override fun skipToIndex(index: Int) {
        mainScope.launch {
            val ctrl = awaitController()
            val last = ctrl.mediaItemCount - 1
            if (last < 0) return@launch
            ctrl.seekTo(index.coerceIn(0, last), /* positionMs = */ 0L)
        }
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        mainScope.launch {
            awaitController().shuffleModeEnabled = enabled
        }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        mainScope.launch {
            awaitController().repeatMode = RepeatModeMapper.toPlayer(mode)
        }
    }

    override fun stop() {
        mainScope.launch {
            val ctrl = controller ?: return@launch
            ctrl.stop()
            ctrl.clearMediaItems()
            _state.value = PlaybackState()
        }
    }

    // ---- internals ----

    private suspend fun awaitController(): MediaController {
        controller?.let { return it }

        return suspendCancellableCoroutine { cont ->
            val token = SessionToken(
                context,
                ComponentName(context, MusikkkPlaybackService::class.java),
            )
            val future = MediaController.Builder(context, token).buildAsync()
            controllerFuture = future

            future.addListener(
                {
                    val ctrl = future.get()
                    controller = ctrl
                    attachListener(ctrl)
                    syncFromPlayer(ctrl)
                    cont.resume(ctrl)
                },
                ContextCompat.getMainExecutor(context),
            )

            cont.invokeOnCancellation { future.cancel(false) }
        }
    }

    private fun attachListener(ctrl: MediaController) {
        ctrl.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                syncFromPlayer(player)
            }
        })
    }

    private fun syncFromPlayer(player: Player) {
        val isPlaying = player.isPlaying
        val isBuffering = player.playbackState == Player.STATE_BUFFERING
        val currentIndex = player.currentMediaItemIndex
        val durationMs = player.duration.takeIf { it > 0 } ?: 0L
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val hasError = player.playerError != null

        _state.update {
            it.copy(
                currentIndex = if (it.queue.isNotEmpty()) currentIndex.coerceAtMost(it.queue.lastIndex) else -1,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                positionMs = positionMs,
                durationMs = durationMs,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = RepeatModeMapper.fromPlayer(player.repeatMode),
                hasFatalError = hasError,
            )
        }

        managePositionTicker(isPlaying)
    }

    private fun managePositionTicker(isPlaying: Boolean) {
        if (isPlaying) {
            if (positionJob?.isActive == true) return
            positionJob = mainScope.launch {
                while (true) {
                    delay(POSITION_TICK_MS)
                    val ctrl = controller ?: break
                    _state.update { it.copy(positionMs = ctrl.currentPosition.coerceAtLeast(0L)) }
                }
            }
        } else {
            positionJob?.cancel()
            positionJob = null
        }
    }

    private fun PlayableTrack.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artistName)
            .setAlbumTitle(albumName)
            .build()

        return MediaItem.Builder()
            .setMediaId(blobId)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    private companion object {
        const val POSITION_TICK_MS = 500L
    }
}
