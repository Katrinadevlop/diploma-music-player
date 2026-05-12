package ru.musikkk.player.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.musikkk.player.R
import ru.musikkk.player.domain.playback.PlaybackState
import ru.musikkk.player.domain.playback.RepeatMode
import ru.musikkk.player.ui.components.CoverImage
import ru.musikkk.player.ui.format.formatDuration
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onClose: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var queueVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = MusikkkSpacing.s5),
    ) {
        TopBar(onClose = onClose, onOpenQueue = { queueVisible = true })

        val track = state.currentTrack
        if (track == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Spacer(Modifier.height(MusikkkSpacing.s5))

        CoverImage(
            coverId = track.coverId,
            contentDescription = track.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            fallbackText = track.title,
            radius = MusikkkRadius.xl,
        )

        Spacer(Modifier.height(MusikkkSpacing.s5))

        Text(
            text = track.title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MusikkkSpacing.s1))
        Text(
            text = track.artistName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(MusikkkSpacing.s4))

        ProgressRow(state = state, onSeek = viewModel::seekTo)

        Spacer(Modifier.height(MusikkkSpacing.s3))

        ControlsRow(
            state = state,
            onTogglePlay = viewModel::togglePlayPause,
            onSkipPrev = viewModel::skipPrevious,
            onSkipNext = viewModel::skipNext,
            onToggleShuffle = viewModel::toggleShuffle,
            onCycleRepeat = viewModel::cycleRepeatMode,
        )
    }

    if (queueVisible) {
        QueueSheet(
            state = state,
            onDismiss = { queueVisible = false },
            onTrackClick = { index ->
                viewModel.skipToIndex(index)
                queueVisible = false
            },
        )
    }
}

@Composable
private fun TopBar(onClose: () -> Unit, onOpenQueue: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.player_close),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(id = R.string.player_now_playing),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenQueue) {
            Icon(
                imageVector = Icons.Filled.QueueMusic,
                contentDescription = stringResource(id = R.string.player_queue),
            )
        }
    }
}

@Composable
private fun ProgressRow(state: PlaybackState, onSeek: (Long) -> Unit) {
    // Локальный drag-state: пока пользователь тащит ползунок, не дёргаем
    // плеер — обновляем только UI, а seek выполняем на отпускание.
    var dragValue by remember { mutableStateOf<Float?>(null) }

    val durationF = state.durationMs.toFloat().coerceAtLeast(1f)
    val currentF = (dragValue ?: state.positionMs.toFloat()).coerceIn(0f, durationF)

    Slider(
        value = currentF,
        onValueChange = { dragValue = it },
        onValueChangeFinished = {
            dragValue?.let { onSeek(it.toLong()) }
            dragValue = null
        },
        valueRange = 0f..durationF,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            thumbColor = MaterialTheme.colorScheme.primary,
        ),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatDuration((currentF / 1000f).toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatDuration((state.durationMs / 1000L).toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ControlsRow(
    state: PlaybackState,
    onTogglePlay: () -> Unit,
    onSkipPrev: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = stringResource(id = R.string.player_shuffle),
                tint = if (state.shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        IconButton(onClick = onSkipPrev) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(id = R.string.player_skip_previous),
                modifier = Modifier.size(36.dp),
            )
        }

        BigPlayPauseButton(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            onClick = onTogglePlay,
        )

        IconButton(onClick = onSkipNext) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(id = R.string.player_skip_next),
                modifier = Modifier.size(36.dp),
            )
        }

        IconButton(onClick = onCycleRepeat) {
            val (icon, tint) = when (state.repeatMode) {
                RepeatMode.Off -> Icons.Filled.Repeat to MaterialTheme.colorScheme.onSurfaceVariant
                RepeatMode.All -> Icons.Filled.Repeat to MaterialTheme.colorScheme.primary
                RepeatMode.One -> Icons.Filled.RepeatOne to MaterialTheme.colorScheme.primary
            }
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = R.string.player_repeat),
                tint = tint,
            )
        }
    }
}

@Composable
private fun BigPlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
    ) {
        when {
            isBuffering -> CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
            )
            else -> Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    id = if (isPlaying) R.string.player_pause else R.string.player_play,
                ),
                tint = Color.Unspecified,
                modifier = Modifier.size(56.dp),
            )
        }
    }
}
