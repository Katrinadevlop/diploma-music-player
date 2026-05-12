package ru.musikkk.player.feature.userdata

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
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
import ru.musikkk.player.data.user.PlayPathsAction
import ru.musikkk.player.data.user.PlaycountsRepository
import ru.musikkk.player.domain.library.Track

data class TopUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TopViewModel @Inject constructor(
    private val playcountsRepository: PlaycountsRepository,
    private val libraryRepository: LibraryRepository,
    private val playPaths: PlayPathsAction,
) : ViewModel() {

    val state: StateFlow<TopUiState> = playcountsRepository.counts
        .map { counts ->
            val sortedPaths = counts.entries
                .sortedByDescending { it.value }
                .take(100)
                .map { it.key }
            val tracks = libraryRepository.tracksByPaths(sortedPaths)
            TopUiState(tracks = tracks, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = TopUiState(),
        )

    init {
        viewModelScope.launch { runCatching { playcountsRepository.refresh() } }
    }

    fun playFromIndex(index: Int) {
        val paths = state.value.tracks.map { it.filePath }
        viewModelScope.launch { playPaths(paths = paths, startIndex = index) }
    }
}

@Composable
fun TopScreen(
    onBack: () -> Unit,
    viewModel: TopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UserSectionScaffold(
        title = stringResource(id = R.string.hub_top),
        sectionIcon = Icons.Filled.Whatshot,
        onBack = onBack,
        isLoading = state.isLoading,
        tracks = state.tracks,
        emptyMessage = stringResource(id = R.string.top_empty),
        onTrackClick = viewModel::playFromIndex,
    )
}
