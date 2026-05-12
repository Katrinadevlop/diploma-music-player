package ru.musikkk.player.feature.release

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.Track

data class ReleaseDetailUiState(
    val release: Release? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ReleaseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    libraryRepository: LibraryRepository,
) : ViewModel() {

    private val releaseId: String =
        Uri.decode(savedStateHandle.get<String>(ARG_RELEASE_ID).orEmpty())

    val state: StateFlow<ReleaseDetailUiState> = combine(
        libraryRepository.observeRelease(releaseId),
        libraryRepository.observeReleaseTracks(releaseId),
    ) { release, tracks ->
        ReleaseDetailUiState(
            release = release,
            tracks = tracks,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = ReleaseDetailUiState(),
    )

    companion object {
        const val ARG_RELEASE_ID = "releaseId"
    }
}
