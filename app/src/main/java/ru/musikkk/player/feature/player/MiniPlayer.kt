package ru.musikkk.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.musikkk.player.R
import ru.musikkk.player.domain.playback.PlaybackState
import ru.musikkk.player.ui.components.CoverImage
import ru.musikkk.player.ui.theme.MusikkkColors
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

private val MiniPlayerHeight = 72.dp

@Composable
fun MiniPlayer(
    state: PlaybackState,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack ?: return

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .background(MusikkkColors.SurfaceElevated)
                .clickable(onClick = onExpand),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MusikkkSpacing.s3, vertical = MusikkkSpacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(
                    coverId = track.coverId,
                    contentDescription = track.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(MusikkkRadius.sm)),
                    fallbackText = track.title,
                    radius = MusikkkRadius.sm,
                )
                Spacer(Modifier.size(MusikkkSpacing.s3))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                PlayPauseButton(
                    isPlaying = state.isPlaying,
                    isBuffering = state.isBuffering,
                    onClick = onTogglePlay,
                )

                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(id = R.string.player_skip_next),
                    )
                }
            }
        }

        // Тонкая полоска прогресса по нижней кромке мини-плеера.
        val progress = if (state.durationMs > 0) {
            (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        when {
            isBuffering -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            else -> Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    id = if (isPlaying) R.string.player_pause else R.string.player_play,
                ),
            )
        }
    }
}
