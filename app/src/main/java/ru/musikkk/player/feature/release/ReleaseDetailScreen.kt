package ru.musikkk.player.feature.release

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.musikkk.player.R
import ru.musikkk.player.domain.download.DownloadInfo
import ru.musikkk.player.domain.download.DownloadStatus
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.Track
import ru.musikkk.player.ui.components.CoverImage
import ru.musikkk.player.ui.format.formatDuration
import ru.musikkk.player.ui.format.formatReleaseYear
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDetailScreen(
    onBack: () -> Unit,
    viewModel: ReleaseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.release?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.release_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.release == null -> Text(
                    text = stringResource(id = R.string.release_not_found),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(MusikkkSpacing.s5),
                )
                else -> Content(
                    release = state.release!!,
                    tracks = state.tracks,
                    downloads = state.downloads,
                    onTrackClick = viewModel::playFromIndex,
                    onDownloadAction = viewModel::onDownloadAction,
                )
            }
        }
    }
}

@Composable
private fun Content(
    release: Release,
    tracks: List<Track>,
    downloads: Map<String, DownloadInfo>,
    onTrackClick: (index: Int) -> Unit,
    onDownloadAction: (Track) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "header") {
            Header(release = release, trackCount = tracks.size)
        }
        itemsIndexed(tracks, key = { _, t -> t.blobId }) { index, track ->
            TrackRow(
                track = track,
                download = downloads[track.blobId],
                onClick = { onTrackClick(index) },
                onDownloadAction = { onDownloadAction(track) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 88.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun Header(release: Release, trackCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MusikkkSpacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverImage(
            coverId = release.coverId,
            contentDescription = release.name,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f),
            fallbackText = release.name,
            radius = MusikkkRadius.lg,
        )
        Spacer(Modifier.height(MusikkkSpacing.s4))
        Text(
            text = release.name,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(MusikkkSpacing.s1))
        Text(
            text = release.artistName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val year = formatReleaseYear(release.releaseDate, release.year)
        val countText = pluralizeTracks(trackCount)
        val sub = listOfNotNull(year, countText).joinToString(" · ")
        if (sub.isNotEmpty()) {
            Spacer(Modifier.height(MusikkkSpacing.s1))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(MusikkkSpacing.s4))
    }
}

@Composable
private fun TrackRow(
    track: Track,
    download: DownloadInfo?,
    onClick: () -> Unit,
    onDownloadAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = track.trackNumber?.toString()
                    ?: stringResource(id = R.string.release_track_number_placeholder),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(MusikkkSpacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (track.artistName.isNotBlank() && track.artistName != track.albumName) {
                Text(
                    text = track.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.size(MusikkkSpacing.s2))

        DownloadIconButton(download = download, onClick = onDownloadAction)

        Spacer(Modifier.size(MusikkkSpacing.s2))
        Text(
            text = formatDuration(track.duration),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DownloadIconButton(
    download: DownloadInfo?,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        when (download?.status) {
            null -> Icon(
                imageVector = Icons.Filled.DownloadForOffline,
                contentDescription = stringResource(id = R.string.download_action_start),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DownloadStatus.Queued -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            DownloadStatus.Running -> CircularProgressIndicator(
                progress = { download.progress },
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            DownloadStatus.Completed -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(id = R.string.download_action_delete),
                tint = MaterialTheme.colorScheme.primary,
            )
            DownloadStatus.Failed -> Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = stringResource(id = R.string.download_action_retry),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun pluralizeTracks(count: Int): String {
    val resId = when {
        count == 0 -> R.string.library_track_count_zero
        count == 1 -> R.string.library_track_count_one
        else -> R.string.library_track_count_other
    }
    return if (resId == R.string.library_track_count_zero) {
        stringResource(id = resId)
    } else {
        stringResource(id = resId, count)
    }
}
