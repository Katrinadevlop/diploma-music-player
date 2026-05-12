package ru.musikkk.player.data.playback

import kotlinx.coroutines.flow.StateFlow
import ru.musikkk.player.domain.playback.PlayableTrack
import ru.musikkk.player.domain.playback.PlaybackState
import ru.musikkk.player.domain.playback.RepeatMode

interface PlaybackController {
    /** Текущее состояние плеера — для подписки из UI. */
    val state: StateFlow<PlaybackState>

    /**
     * Поставить очередь и начать воспроизведение с указанного индекса.
     * `startPositionMs` — позиция внутри стартового трека (для возврата
     * к Continue). По умолчанию 0.
     */
    fun playQueue(queue: List<PlayableTrack>, startIndex: Int, startPositionMs: Long = 0L)

    fun togglePlayPause()
    fun seekToMs(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun skipToIndex(index: Int)

    fun setShuffleEnabled(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)

    fun stop()
}
