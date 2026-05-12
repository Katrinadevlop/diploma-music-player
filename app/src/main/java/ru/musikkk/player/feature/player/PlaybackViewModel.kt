package ru.musikkk.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ru.musikkk.player.data.playback.PlaybackController
import ru.musikkk.player.domain.playback.PlaybackState
import ru.musikkk.player.domain.playback.RepeatMode

/**
 * Узкий мост между UI-плеера и [PlaybackController]. Сам по себе
 * controller — Hilt singleton, поэтому несколько VM могут спокойно
 * читать его state параллельно (mini-player + full-player + queue).
 */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val state: StateFlow<PlaybackState> = playbackController.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = PlaybackState(),
        )

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun skipNext() = playbackController.skipToNext()

    fun skipPrevious() = playbackController.skipToPrevious()

    fun skipToIndex(index: Int) = playbackController.skipToIndex(index)

    fun seekTo(positionMs: Long) = playbackController.seekToMs(positionMs)

    fun toggleShuffle() {
        playbackController.setShuffleEnabled(!state.value.shuffleEnabled)
    }

    /** Циклически переключает Off → All → One → Off. */
    fun cycleRepeatMode() {
        val next = when (state.value.repeatMode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        playbackController.setRepeatMode(next)
    }
}
