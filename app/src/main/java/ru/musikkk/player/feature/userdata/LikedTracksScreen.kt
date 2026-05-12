package ru.musikkk.player.feature.userdata

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
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
import ru.musikkk.player.data.user.LikesRepository
import ru.musikkk.player.data.user.PlayPathsAction
import ru.musikkk.player.domain.library.Track
import ru.musikkk.player.ui.components.TrackListRow
import ru.musikkk.player.ui.theme.MusikkkSpacing

data class LikedTracksUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class LikedTracksViewModel @Inject constructor(
    private val likesRepository: LikesRepository,
    private val libraryRepository: LibraryRepository,
    private val playPaths: PlayPathsAction,
) : ViewModel() {

    val state: StateFlow<LikedTracksUiState> = likesRepository.liked
        .map { paths ->
            val tracks = libraryRepository.tracksByPaths(paths.toList())
            LikedTracksUiState(tracks = tracks, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = LikedTracksUiState(),
        )

    init {
        viewModelScope.launch { runCatching { likesRepository.refresh() } }
    }

    fun playFromIndex(index: Int) {
        val paths = state.value.tracks.map { it.filePath }
        viewModelScope.launch { playPaths(paths = paths, startIndex = index) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedTracksScreen(
    onBack: () -> Unit,
    viewModel: LikedTracksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.hub_liked)) },
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
                state.tracks.isEmpty() -> Text(
                    text = stringResource(id = R.string.liked_empty),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(MusikkkSpacing.s5),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(state.tracks, key = { _, t -> t.blobId }) { index, track ->
                        TrackListRow(track = track, onClick = { viewModel.playFromIndex(index) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
