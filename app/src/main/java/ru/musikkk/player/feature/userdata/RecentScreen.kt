package ru.musikkk.player.feature.userdata

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
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
import ru.musikkk.player.data.user.RecentRepository
import ru.musikkk.player.domain.library.Track

data class RecentUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val recentRepository: RecentRepository,
    private val libraryRepository: LibraryRepository,
    private val playPaths: PlayPathsAction,
) : ViewModel() {

    val state: StateFlow<RecentUiState> = recentRepository.recent
        .map { entries ->
            val tracks = libraryRepository.tracksByPaths(entries.map { it.trackPath })
            RecentUiState(tracks = tracks, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = RecentUiState(),
        )

    init {
        viewModelScope.launch { runCatching { recentRepository.refresh() } }
    }

    fun playFromIndex(index: Int) {
        val paths = state.value.tracks.map { it.filePath }
        viewModelScope.launch { playPaths(paths = paths, startIndex = index) }
    }
}

@Composable
fun RecentScreen(
    onBack: () -> Unit,
    viewModel: RecentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UserSectionScaffold(
        title = stringResource(id = R.string.hub_recent),
        sectionIcon = Icons.Filled.History,
        onBack = onBack,
        isLoading = state.isLoading,
        tracks = state.tracks,
        emptyMessage = stringResource(id = R.string.recent_empty),
        onTrackClick = viewModel::playFromIndex,
    )
}
