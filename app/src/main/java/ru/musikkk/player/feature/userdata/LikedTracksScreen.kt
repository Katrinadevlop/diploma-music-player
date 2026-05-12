package ru.musikkk.player.feature.userdata

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import ru.musikkk.player.ui.components.LikesPatternOverlay

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

@Composable
fun LikedTracksScreen(
    onBack: () -> Unit,
    viewModel: LikedTracksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UserSectionScaffold(
        title = stringResource(id = R.string.hub_liked),
        sectionIcon = Icons.Filled.Favorite,
        onBack = onBack,
        isLoading = state.isLoading,
        tracks = state.tracks,
        emptyMessage = stringResource(id = R.string.liked_empty),
        onTrackClick = viewModel::playFromIndex,
        // На вебе у Liked-страницы свой «обоиный» фон с точками
        // (`likes_bg.svg`) — повторяем его вместо стандартного backdrop'a.
        backdrop = { LikesPatternOverlay() },
    )
}
