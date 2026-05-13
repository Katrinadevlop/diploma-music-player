package ru.musikkk.player.feature.userdata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.musikkk.player.R
import ru.musikkk.player.domain.library.Track
import ru.musikkk.player.ui.util.tracksCountString
import ru.musikkk.player.ui.components.PlaybackAwareBackdrop
import ru.musikkk.player.ui.components.SectionCover
import ru.musikkk.player.ui.components.TrackListRow
import ru.musikkk.player.ui.theme.MusikkkSpacing

/**
 * Общий шаблон для секционных экранов (Liked / Recent / Top):
 * backdrop с обложкой текущего трека за всем UI, шапка с большой
 * иконической «обложкой» секции (как `likes_default.svg` /
 * `recent_default.svg` на вебе), и LazyColumn треков.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserSectionScaffold(
    title: String,
    sectionIcon: ImageVector,
    onBack: () -> Unit,
    isLoading: Boolean,
    tracks: List<Track>,
    emptyMessage: String,
    onTrackClick: (index: Int) -> Unit,
    backdrop: @Composable () -> Unit = {
        PlaybackAwareBackdrop(fallbackCoverId = tracks.firstOrNull()?.coverId)
    },
) {
    Box(modifier = Modifier.fillMaxSize()) {
        backdrop()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.release_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
            containerColor = Color.Transparent,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    tracks.isEmpty() -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MusikkkSpacing.s5),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        SectionCover(
                            icon = sectionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(140.dp),
                        )
                        Spacer(Modifier.height(MusikkkSpacing.s4))
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item(key = "section_header") {
                            SectionHeader(
                                title = title,
                                icon = sectionIcon,
                                trackCount = tracks.size,
                            )
                        }
                        itemsIndexed(tracks, key = { _, t -> t.blobId }) { index, track ->
                            TrackListRow(track = track, onClick = { onTrackClick(index) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    trackCount: Int,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MusikkkSpacing.s5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionCover(
            icon = icon,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.size(MusikkkSpacing.s4))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tracksCountString(trackCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

