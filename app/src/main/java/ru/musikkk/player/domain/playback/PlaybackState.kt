package ru.musikkk.player.domain.playback

/** Один трек в очереди плеера. Минимум полей, нужных для UI и стриминга. */
data class PlayableTrack(
    val blobId: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val coverId: String?,
    val durationSeconds: Int,
    val streamUrl: String,
)

/** Режим повтора. Соответствует константам `Player.REPEAT_MODE_*` в Media3. */
enum class RepeatMode { Off, One, All }

/** Состояние плеера, которое UI читает из [ru.musikkk.player.data.playback.PlaybackController]. */
data class PlaybackState(
    val queue: List<PlayableTrack> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val hasFatalError: Boolean = false,
) {
    val currentTrack: PlayableTrack?
        get() = queue.getOrNull(currentIndex)

    val isActive: Boolean
        get() = currentTrack != null
}
