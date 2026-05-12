package ru.musikkk.player.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ru.musikkk.player.R
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.ui.components.CoverImage
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSignedOut: () -> Unit,
    onReleaseClick: (Release) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUploads: () -> Unit,
    onOpenHub: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val events = viewModel.events

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                LibraryEvent.SignedOut -> onSignedOut()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.library_title)) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(id = R.string.search_title),
                        )
                    }
                    IconButton(onClick = onOpenHub) {
                        Icon(
                            imageVector = Icons.Filled.LibraryMusic,
                            contentDescription = stringResource(id = R.string.hub_title),
                        )
                    }
                    IconButton(onClick = onOpenUploads) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = stringResource(id = R.string.uploads_title),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.settings_title),
                        )
                    }
                    TextButton(onClick = viewModel::logout) {
                        Text(stringResource(id = R.string.auth_action_logout))
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
                state.isInitialLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.errorRes != null && state.releases.isEmpty() -> {
                    ErrorBlock(
                        title = stringResource(id = R.string.library_error_title),
                        message = stringResource(id = state.errorRes!!),
                        onRetry = viewModel::retry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                state.showEmptyState -> {
                    EmptyBlock(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.isRefreshing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        state.continueCard?.let { card ->
                            ContinueCardView(
                                card = card,
                                onResume = viewModel::resumeContinue,
                                modifier = Modifier
                                    .padding(
                                        horizontal = MusikkkSpacing.s4,
                                        vertical = MusikkkSpacing.s2,
                                    )
                                    .fillMaxWidth(),
                            )
                        }
                        ReleasesGrid(
                            releases = state.releases,
                            onReleaseClick = onReleaseClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleasesGrid(
    releases: List<Release>,
    onReleaseClick: (Release) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MusikkkSpacing.s4,
            end = MusikkkSpacing.s4,
            top = MusikkkSpacing.s3,
            bottom = MusikkkSpacing.s6,
        ),
        horizontalArrangement = Arrangement.spacedBy(MusikkkSpacing.s3),
        verticalArrangement = Arrangement.spacedBy(MusikkkSpacing.s4),
    ) {
        items(releases, key = { it.id }) { release ->
            ReleaseCard(release = release, onClick = { onReleaseClick(release) })
        }
    }
}

@Composable
private fun ReleaseCard(
    release: Release,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        CoverImage(
            coverId = release.coverId,
            contentDescription = release.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            fallbackText = release.name,
            radius = MusikkkRadius.md,
        )
        Spacer(Modifier.height(MusikkkSpacing.s2))
        Text(
            text = release.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = release.artistName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ErrorBlock(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(MusikkkSpacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(MusikkkSpacing.s2))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MusikkkSpacing.s4))
        OutlinedButton(onClick = onRetry) {
            Text(stringResource(id = R.string.common_retry))
        }
    }
}

@Composable
private fun ContinueCardView(
    card: ContinueCard,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .clickable(onClick = onResume)
            .padding(MusikkkSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverId = card.track.coverId,
            contentDescription = card.track.title,
            modifier = Modifier.size(56.dp),
            fallbackText = card.track.title,
            radius = MusikkkRadius.sm,
        )
        Spacer(Modifier.width(MusikkkSpacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.continue_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = card.track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.track.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = stringResource(id = R.string.player_play),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun EmptyBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(MusikkkSpacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.library_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(MusikkkSpacing.s2))
        Text(
            text = stringResource(id = R.string.library_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
