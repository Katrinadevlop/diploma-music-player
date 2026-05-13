package ru.musikkk.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.musikkk.player.R
import ru.musikkk.player.domain.playback.PlayableTrack
import ru.musikkk.player.domain.playback.PlaybackState
import ru.musikkk.player.ui.components.CoverImage
import ru.musikkk.player.ui.format.formatDuration
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlaybackState,
    onDismiss: () -> Unit,
    onTrackClick: (index: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.player_queue),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    start = MusikkkSpacing.s5,
                    end = MusikkkSpacing.s5,
                    top = MusikkkSpacing.s3,
                    bottom = MusikkkSpacing.s3,
                ),
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(state.queue, key = { _, t -> t.blobId }) { index, track ->
                    QueueRow(
                        track = track,
                        isCurrent = index == state.currentIndex,
                        onClick = { onTrackClick(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    track: PlayableTrack,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val rowBackground = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(rowBackground)
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCurrent) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(MusikkkSpacing.s2))
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(MusikkkRadius.sm)),
            ) {
                CoverImage(
                    coverId = track.coverId,
                    contentDescription = track.title,
                    modifier = Modifier.size(40.dp),
                    fallbackText = track.title,
                    radius = MusikkkRadius.sm,
                )
            }
            Spacer(Modifier.width(MusikkkSpacing.s3))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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

        Spacer(Modifier.width(MusikkkSpacing.s2))
        Text(
            text = formatDuration(track.durationSeconds),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
