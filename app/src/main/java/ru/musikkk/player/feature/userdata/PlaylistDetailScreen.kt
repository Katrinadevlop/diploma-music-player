package ru.musikkk.player.feature.userdata

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.musikkk.player.R
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.data.user.PlayPathsAction
import ru.musikkk.player.data.user.PlaylistsRepository
import ru.musikkk.player.domain.library.Track
import ru.musikkk.player.domain.user.Playlist
import ru.musikkk.player.ui.components.TrackListRow

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistsRepository: PlaylistsRepository,
    private val libraryRepository: LibraryRepository,
    private val playPaths: PlayPathsAction,
) : ViewModel() {

    private val playlistId: String = Uri.decode(savedStateHandle.get<String>(ARG_PLAYLIST_ID).orEmpty())

    val state: StateFlow<PlaylistDetailUiState> = playlistsRepository.playlists
        .map { all ->
            val pl = all.firstOrNull { it.id == playlistId }
            if (pl == null) {
                PlaylistDetailUiState(playlist = null, tracks = emptyList(), isLoading = false)
            } else {
                val tracks = libraryRepository.tracksByPaths(pl.trackPaths)
                PlaylistDetailUiState(playlist = pl, tracks = tracks, isLoading = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = PlaylistDetailUiState(),
        )

    init {
        viewModelScope.launch { runCatching { playlistsRepository.refresh() } }
    }

    fun playFromIndex(index: Int) {
        val tracks = state.value.tracks
        if (index !in tracks.indices) return
        val paths = tracks.map { it.filePath }
        viewModelScope.launch { playPaths(paths = paths, startIndex = index) }
    }

    fun removeTrack(trackPath: String) {
        viewModelScope.launch {
            runCatching { playlistsRepository.removeTrack(playlistId, trackPath) }
        }
    }

    companion object {
        const val ARG_PLAYLIST_ID = "playlistId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.playlist?.name.orEmpty(),
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
                state.playlist == null -> Text(
                    text = stringResource(id = R.string.playlists_not_found),
                    modifier = Modifier.align(Alignment.Center),
                )
                state.tracks.isEmpty() -> Text(
                    text = stringResource(id = R.string.playlist_detail_empty),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(state.tracks, key = { _, t -> t.blobId }) { index, track ->
                        TrackListRow(
                            track = track,
                            onClick = { viewModel.playFromIndex(index) },
                            trailing = {
                                IconButton(onClick = { viewModel.removeTrack(track.filePath) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(id = R.string.playlist_remove_track),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
