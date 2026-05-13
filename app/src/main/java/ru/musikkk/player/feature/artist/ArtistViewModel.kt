package ru.musikkk.player.feature.artist

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
import kotlinx.coroutines.launch
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.domain.library.Artist
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.ReleaseSection

/**
 * Состояние экрана артиста. Релизы заранее разбиты по секциям,
 * чтобы UI просто перебрал готовые группы — никакой логики
 * `groupBy { it.section }` в Compose-функциях.
 */
data class ArtistUiState(
    val artist: Artist? = null,
    val albums: List<Release> = emptyList(),
    val eps: List<Release> = emptyList(),
    val singles: List<Release> = emptyList(),
    val collabs: List<Release> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    /**
     * id артиста приходит через path-параметр и может содержать символы
     * (пробелы, кавычки), которые `Uri.encode` сделал безопасными для
     * сегмента URI. Декодим обратно перед чтением из БД.
     */
    private val artistId: String =
        Uri.decode(savedStateHandle.get<String>(ARG_ARTIST_ID).orEmpty())

    val state: StateFlow<ArtistUiState> = combine(
        libraryRepository.observeArtist(artistId),
        libraryRepository.observeReleasesByArtist(artistId),
    ) { artist, releases ->
        ArtistUiState(
            artist = artist,
            albums = releases.filter { it.section == ReleaseSection.Album },
            eps = releases.filter { it.section == ReleaseSection.Ep },
            singles = releases.filter { it.section == ReleaseSection.Single },
            collabs = releases.filter { it.section == ReleaseSection.Collab },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = ArtistUiState(),
    )

    init {
        // Подтягиваем свежий снапшот библиотеки в фоне: если артист
        // только что появился (свежий upload, другая сессия), он
        // попадёт в кэш и подкупится наш combine.
        viewModelScope.launch { runCatching { libraryRepository.refresh() } }
    }

    companion object {
        const val ARG_ARTIST_ID = "artistId"
    }
}
