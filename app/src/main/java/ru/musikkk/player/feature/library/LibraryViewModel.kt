package ru.musikkk.player.feature.library

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException
import ru.musikkk.player.R
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.domain.library.Release

data class LibraryUiState(
    val releases: List<Release> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    @StringRes val errorRes: Int? = null,
) {
    val showEmptyState: Boolean
        get() = !isInitialLoading && errorRes == null && releases.isEmpty()
}

sealed class LibraryEvent {
    data object SignedOut : LibraryEvent()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState())

    val state: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeReleases(),
        refreshState,
    ) { releases, refresh ->
        LibraryUiState(
            releases = releases,
            isInitialLoading = refresh.isInitial,
            isRefreshing = refresh.isRefreshing,
            errorRes = refresh.errorRes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = LibraryUiState(),
    )

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh(initial = true)
    }

    fun retry() {
        refresh(initial = state.value.releases.isEmpty())
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _events.send(LibraryEvent.SignedOut)
        }
    }

    private fun refresh(initial: Boolean) {
        refreshState.update {
            it.copy(
                isInitial = initial,
                isRefreshing = !initial,
                errorRes = null,
            )
        }

        viewModelScope.launch {
            val result = runCatching { libraryRepository.refresh() }

            result
                .onSuccess {
                    refreshState.update {
                        it.copy(isInitial = false, isRefreshing = false, errorRes = null)
                    }
                }
                .onFailure { e ->
                    if (e is HttpException && e.code() == 401) {
                        authRepository.logout()
                        _events.send(LibraryEvent.SignedOut)
                        return@onFailure
                    }
                    val res = when (e) {
                        is IOException -> R.string.auth_error_network
                        else -> R.string.library_error_title
                    }
                    refreshState.update {
                        it.copy(isInitial = false, isRefreshing = false, errorRes = res)
                    }
                }
        }
    }

    private data class RefreshState(
        val isInitial: Boolean = true,
        val isRefreshing: Boolean = false,
        @StringRes val errorRes: Int? = null,
    )
}
