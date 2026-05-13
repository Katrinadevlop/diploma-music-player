package ru.musikkk.player.feature.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException
import ru.musikkk.player.R
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.data.user.ContinueRepository
import ru.musikkk.player.data.user.LikesRepository
import ru.musikkk.player.data.user.PlayPathsAction
import ru.musikkk.player.data.user.PlaycountsRepository
import ru.musikkk.player.data.user.PlaylistsRepository
import ru.musikkk.player.data.user.RecentRepository
import ru.musikkk.player.domain.library.Artist
import ru.musikkk.player.domain.user.ContinueState
import ru.musikkk.player.domain.user.Playlist

/**
 * Тайл «Для тебя»-секции. `coverId` — обложка первого/последнего трека
 * в наборе, или `null` если данных ещё нет (тогда рисуем иконку-fallback).
 */
data class SmartTile(
    val kind: SmartKind,
    val coverId: String?,
)

enum class SmartKind { Continue, Recent, Top }

/** Заголовок «Лайки» рендерится отдельным тайлом, поэтому ему нужна обложка. */
data class LikesTile(
    val trackCount: Int,
    val coverId: String?,
)

data class HomeUiState(
    val smartTiles: List<SmartTile> = emptyList(),
    val likesTile: LikesTile = LikesTile(trackCount = 0, coverId = null),
    val playlists: List<Playlist> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val continueState: ContinueState? = null,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    @StringRes val errorRes: Int? = null,
) {
    val isEmpty: Boolean
        get() = playlists.isEmpty() && artists.isEmpty() && likesTile.trackCount == 0
}

sealed class HomeEvent {
    data object SignedOut : HomeEvent()
}

/**
 * Главный экран — Smart-секция (Continue/Recent/Top) + Плейлисты +
 * Артисты, по образцу `view-artists` веб-клиента. Continue хранится
 * отдельно: ему нужен сам трек, чтобы плеер мог стартовать с
 * сохранённой позиции (см. [resumeContinue]).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val authRepository: AuthRepository,
    private val continueRepository: ContinueRepository,
    private val recentRepository: RecentRepository,
    private val playcountsRepository: PlaycountsRepository,
    private val likesRepository: LikesRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val playPaths: PlayPathsAction,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshState())

    val state: StateFlow<HomeUiState> = combine(
        continueRepository.state,
        recentRepository.recent,
        playcountsRepository.counts,
        likesRepository.liked,
        playlistsRepository.playlists,
        libraryRepository.observeArtists(),
        refreshState,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val cont = values[0] as ContinueState?
        @Suppress("UNCHECKED_CAST")
        val recent = values[1] as List<ru.musikkk.player.domain.user.RecentEntry>
        @Suppress("UNCHECKED_CAST")
        val counts = values[2] as Map<String, Int>
        @Suppress("UNCHECKED_CAST")
        val liked = values[3] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val playlists = values[4] as List<Playlist>
        @Suppress("UNCHECKED_CAST")
        val artists = values[5] as List<Artist>
        val refresh = values[6] as RefreshState

        // Резолвим обложки для тайлов одним пакетным запросом — путь сразу
        // в Map, чтобы не делать N suspend-вызовов на каждый emit.
        val pathsToResolve = buildSet {
            cont?.trackPath?.let { add(it) }
            recent.firstOrNull()?.trackPath?.let { add(it) }
            counts.maxByOrNull { it.value }?.key?.let { add(it) }
            liked.lastOrNull()?.let { add(it) }
        }
        val tracksByPath = libraryRepository.tracksByPaths(pathsToResolve.toList())
            .associateBy { it.filePath }

        val smartTiles = buildList {
            add(SmartTile(SmartKind.Continue, cont?.let { tracksByPath[it.trackPath]?.coverId }))
            add(SmartTile(SmartKind.Recent, recent.firstOrNull()?.let { tracksByPath[it.trackPath]?.coverId }))
            add(SmartTile(SmartKind.Top, counts.maxByOrNull { it.value }?.key?.let { tracksByPath[it]?.coverId }))
        }

        val likesTile = LikesTile(
            trackCount = liked.size,
            coverId = liked.lastOrNull()?.let { tracksByPath[it]?.coverId },
        )

        HomeUiState(
            smartTiles = smartTiles,
            likesTile = likesTile,
            playlists = playlists.sortedByDescending { it.updatedAtMs },
            artists = artists,
            continueState = cont,
            isInitialLoading = refresh.isInitial,
            isRefreshing = refresh.isRefreshing,
            errorRes = refresh.errorRes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = HomeUiState(),
    )

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh(initial = true)
    }

    fun retry() {
        refresh(initial = state.value.artists.isEmpty() && state.value.playlists.isEmpty())
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _events.send(HomeEvent.SignedOut)
        }
    }

    fun resumeContinue() {
        val cont = state.value.continueState ?: return
        viewModelScope.launch {
            playPaths(
                paths = listOf(cont.trackPath),
                startIndex = 0,
                startPositionMs = (cont.timeSeconds * 1000.0).toLong(),
            )
        }
    }

    /**
     * Создание плейлиста с главного экрана (диалог из «+»-карточки).
     * Сам диалог живёт в Compose-стейте, ViewModel только дёргает
     * репозиторий — список обновится через `playlistsRepository.playlists`.
     */
    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { runCatching { playlistsRepository.create(trimmed) } }
    }

    private fun refresh(initial: Boolean) {
        refreshState.update {
            it.copy(isInitial = initial, isRefreshing = !initial, errorRes = null)
        }

        viewModelScope.launch {
            // Все пользовательские источники + библиотека параллельно;
            // ошибки глотаем по отдельности, чтобы offline-проблема в одной
            // части не блокировала остальные секции.
            val libraryResult = runCatching { libraryRepository.refresh() }
            runCatching { continueRepository.refresh() }
            runCatching { recentRepository.refresh() }
            runCatching { playcountsRepository.refresh() }
            runCatching { likesRepository.refresh() }
            runCatching { playlistsRepository.refresh() }

            libraryResult
                .onSuccess {
                    refreshState.update {
                        it.copy(isInitial = false, isRefreshing = false, errorRes = null)
                    }
                }
                .onFailure { e ->
                    if (e is HttpException && e.code() == 401) {
                        authRepository.logout()
                        _events.send(HomeEvent.SignedOut)
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
