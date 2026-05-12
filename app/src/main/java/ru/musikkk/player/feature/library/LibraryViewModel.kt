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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException
import ru.musikkk.player.R
import ru.musikkk.player.core.database.dao.DownloadDao
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.data.settings.SettingsRepository
import ru.musikkk.player.data.user.ContinueRepository
import ru.musikkk.player.data.user.PlayPathsAction
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.Track
import ru.musikkk.player.domain.settings.LibraryFilters

data class ContinueCard(
    val track: Track,
    val positionSeconds: Double,
)

data class LibraryUiState(
    val releases: List<Release> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val filters: LibraryFilters = LibraryFilters(),
    val continueCard: ContinueCard? = null,
) {
    val showEmptyState: Boolean
        get() = !isInitialLoading && errorRes == null && releases.isEmpty()
}

sealed class LibraryEvent {
    data object SignedOut : LibraryEvent()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val downloadDao: DownloadDao,
    private val continueRepository: ContinueRepository,
    private val playPaths: PlayPathsAction,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState())

    /**
     * Resolve continue → реальный Track из БД. `flatMapLatest` пересоздаёт
     * подписку, когда меняется continue или библиотека: важно после
     * первого refresh библиотеки — иначе Continue будет ссылаться на
     * `rel_path`, которого ещё нет в Room.
     */
    private val continueCardFlow = combine(
        continueRepository.state,
        libraryRepository.observeReleases(),
    ) { continueState, _ -> continueState }
        .flatMapLatest { continueState ->
            if (continueState == null) flowOf<ContinueCard?>(null)
            else flowOf(
                libraryRepository.trackByPath(continueState.trackPath)?.let { track ->
                    ContinueCard(track = track, positionSeconds = continueState.timeSeconds)
                }
            )
        }

    val state: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeReleases(),
        downloadDao.observeAll(),
        settingsRepository.settingsFlow,
        refreshState,
        continueCardFlow,
    ) { releases, downloads, settings, refresh, continueCard ->
        val downloadedReleaseIds = downloads
            .filter { it.status == STATUS_COMPLETED }
            .mapNotNull { it.releaseId }
            .toSet()
        val filtered = releases.applyFilters(settings.libraryFilters, downloadedReleaseIds)

        LibraryUiState(
            releases = filtered,
            isInitialLoading = refresh.isInitial,
            isRefreshing = refresh.isRefreshing,
            errorRes = refresh.errorRes,
            filters = settings.libraryFilters,
            continueCard = continueCard,
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

    fun resumeContinue() {
        val card = state.value.continueCard ?: return
        viewModelScope.launch {
            playPaths(paths = listOf(card.track.filePath), startIndex = 0)
        }
    }

    private fun refresh(initial: Boolean) {
        refreshState.update {
            it.copy(isInitial = initial, isRefreshing = !initial, errorRes = null)
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

    private fun List<Release>.applyFilters(
        filters: LibraryFilters,
        downloadedReleaseIds: Set<String>,
    ): List<Release> {
        var result = this
        if (filters.sectionFilter != ru.musikkk.player.domain.settings.SectionFilter.All) {
            result = result.filter { filters.sectionFilter.matchesRaw(it.section.raw) }
        }
        if (filters.showOnlyDownloaded) {
            result = result.filter { it.id in downloadedReleaseIds }
        }
        return result
    }

    private data class RefreshState(
        val isInitial: Boolean = true,
        val isRefreshing: Boolean = false,
        @StringRes val errorRes: Int? = null,
    )

    private companion object {
        const val STATUS_COMPLETED = "COMPLETED"
    }
}
