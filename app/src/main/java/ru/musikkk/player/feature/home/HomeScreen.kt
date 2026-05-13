package ru.musikkk.player.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ru.musikkk.player.R
import ru.musikkk.player.domain.library.Artist
import ru.musikkk.player.domain.user.Playlist
import ru.musikkk.player.ui.components.ArtistCard
import ru.musikkk.player.ui.components.HomeTile
import ru.musikkk.player.ui.components.MusikkkTextField
import ru.musikkk.player.ui.components.PlaybackAwareBackdrop
import ru.musikkk.player.ui.theme.MusikkkSpacing

/**
 * Главный экран приложения, аналог `view-artists` в веб-клиенте:
 * три секции — «Для тебя» (Continue/Recent/Top), «Плейлисты»
 * (Лайки + пользовательские + «Создать»), «Артисты». Каждая секция
 * — заголовок на всю ширину + grid карточек, всё в одном
 * LazyVerticalGrid через [GridItemSpan(maxLineSpan)].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSignedOut: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUploads: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenTop: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var createPlaylistVisible by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            when (event) {
                HomeEvent.SignedOut -> onSignedOut()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PlaybackAwareBackdrop()

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
                    state.isInitialLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.errorRes != null && state.isEmpty -> ErrorBlock(
                        title = stringResource(id = R.string.library_error_title),
                        message = stringResource(id = state.errorRes!!),
                        onRetry = viewModel::retry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> Column(modifier = Modifier.fillMaxSize()) {
                        if (state.isRefreshing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        HomeContent(
                            state = state,
                            onResumeContinue = viewModel::resumeContinue,
                            onOpenLiked = onOpenLiked,
                            onOpenRecent = onOpenRecent,
                            onOpenTop = onOpenTop,
                            onOpenPlaylist = onOpenPlaylist,
                            onOpenArtist = onOpenArtist,
                            onCreatePlaylist = { createPlaylistVisible = true },
                        )
                    }
                }
            }
        }
    }

    if (createPlaylistVisible) {
        CreatePlaylistDialog(
            onDismiss = { createPlaylistVisible = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                createPlaylistVisible = false
            },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onResumeContinue: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenTop: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MusikkkSpacing.s4,
            end = MusikkkSpacing.s4,
            top = MusikkkSpacing.s2,
            bottom = MusikkkSpacing.s6,
        ),
        horizontalArrangement = Arrangement.spacedBy(MusikkkSpacing.s3),
        verticalArrangement = Arrangement.spacedBy(MusikkkSpacing.s4),
    ) {
        // ----- Smart-секция -----
        sectionHeader(titleRes = R.string.home_section_smart, key = "smart-header")
        state.smartTiles.forEach { tile ->
            item(key = "smart-${tile.kind.name}") {
                when (tile.kind) {
                    SmartKind.Continue -> HomeTile(
                        title = stringResource(id = R.string.home_smart_continue),
                        subtitle = state.continueState?.trackPath?.substringAfterLast('/')
                            ?: stringResource(id = R.string.home_smart_continue_empty),
                        coverId = tile.coverId,
                        fallbackIcon = Icons.Filled.PlayArrow,
                        onClick = {
                            if (state.continueState != null) onResumeContinue()
                        },
                    )
                    SmartKind.Recent -> HomeTile(
                        title = stringResource(id = R.string.home_smart_recent),
                        subtitle = if (tile.coverId != null) ""
                            else stringResource(id = R.string.home_smart_recent_empty),
                        coverId = tile.coverId,
                        fallbackIcon = Icons.Filled.History,
                        onClick = onOpenRecent,
                    )
                    SmartKind.Top -> HomeTile(
                        title = stringResource(id = R.string.home_smart_top),
                        subtitle = if (tile.coverId != null) ""
                            else stringResource(id = R.string.home_smart_top_empty),
                        coverId = tile.coverId,
                        fallbackIcon = Icons.Filled.Whatshot,
                        onClick = onOpenTop,
                    )
                }
            }
        }

        // ----- Плейлисты -----
        sectionHeader(titleRes = R.string.home_section_playlists, key = "pls-header")
        item(key = "pls-likes") {
            HomeTile(
                title = stringResource(id = R.string.hub_liked),
                subtitle = stringResource(id = R.string.home_likes_subtitle),
                coverId = state.likesTile.coverId,
                fallbackIcon = Icons.Filled.Favorite,
                onClick = onOpenLiked,
            )
        }
        items(state.playlists, key = { "pl-${it.id}" }) { playlist ->
            HomeTile(
                title = playlist.name,
                subtitle = stringResource(id = R.string.home_likes_subtitle),
                coverId = playlist.coverId,
                fallbackIcon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = { onOpenPlaylist(playlist) },
            )
        }
        item(key = "pls-create") {
            HomeTile(
                title = stringResource(id = R.string.home_create_playlist),
                subtitle = stringResource(id = R.string.home_create_playlist_sub),
                fallbackIcon = Icons.Filled.Add,
                onClick = onCreatePlaylist,
            )
        }

        // ----- Артисты -----
        if (state.artists.isNotEmpty()) {
            sectionHeader(titleRes = R.string.home_section_artists, key = "art-header")
            items(state.artists, key = { "art-${it.id}" }) { artist ->
                ArtistCard(artist = artist, onClick = { onOpenArtist(artist) })
            }
        }
    }
}

private fun LazyGridScope.sectionHeader(titleRes: Int, key: String) {
    item(span = { GridItemSpan(maxLineSpan) }, key = key) {
        Text(
            text = androidx.compose.ui.res.stringResource(id = titleRes),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = MusikkkSpacing.s4, bottom = MusikkkSpacing.s2),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.playlists_create_title)) },
        text = {
            MusikkkTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(id = R.string.playlists_name_label),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(id = R.string.playlists_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.common_cancel))
            }
        },
    )
}
