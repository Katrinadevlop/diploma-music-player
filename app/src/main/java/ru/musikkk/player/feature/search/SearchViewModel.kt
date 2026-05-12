package ru.musikkk.player.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.Track

data class SearchUiState(
    val query: String = "",
    val releases: List<Release> = emptyList(),
    val tracks: List<Track> = emptyList(),
) {
    val hasQuery: Boolean
        get() = query.isNotBlank()

    val isEmpty: Boolean
        get() = hasQuery && releases.isEmpty() && tracks.isEmpty()
}

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Дебаунс 300 мс на каждое нажатие клавиши — чтобы не дёргать БД
     * на каждый символ при быстром наборе. После пустого запроса
     * сразу же отдаём пустой результат (без задержки).
     */
    private val activeQuery = _query
        .map { it.trim() }
        .debounce(QUERY_DEBOUNCE_MS)

    private val results = activeQuery.flatMapLatest { trimmed ->
        if (trimmed.isEmpty()) {
            flowOf(emptyList<Release>() to emptyList<Track>())
        } else {
            combine(
                libraryRepository.searchReleases(trimmed),
                libraryRepository.searchTracks(trimmed),
            ) { r, t -> r to t }
        }
    }

    val state: StateFlow<SearchUiState> = combine(_query, results) { q, (releases, tracks) ->
        SearchUiState(query = q, releases = releases, tracks = tracks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = SearchUiState(),
    )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun clear() {
        _query.value = ""
    }

    private companion object {
        const val QUERY_DEBOUNCE_MS = 300L
    }
}
