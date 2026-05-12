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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.musikkk.player.core.network.MediaUrls
import ru.musikkk.player.data.download.DownloadRepository
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.data.playback.PlaybackController
import ru.musikkk.player.domain.download.DownloadInfo
import ru.musikkk.player.domain.download.DownloadStatus
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.Track
import ru.musikkk.player.domain.playback.PlayableTrack

data class ReleaseDetailUiState(
    val release: Release? = null,
    val tracks: List<Track> = emptyList(),
    val downloads: Map<String, DownloadInfo> = emptyMap(),
    val isLoading: Boolean = true,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReleaseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    libraryRepository: LibraryRepository,
    private val downloadRepository: DownloadRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val releaseId: String =
        Uri.decode(savedStateHandle.get<String>(ARG_RELEASE_ID).orEmpty())

    /**
     * Состояние экрана собирается из трёх потоков:
     * 1) метаданные релиза, 2) его треки, 3) состояние скачивания каждого
     * трека. На каждое изменение списка треков пересоздаём подписку на
     * скачивания — иначе после refresh библиотеки можем смотреть на
     * устаревший набор `blob_id`.
     */
    val state: StateFlow<ReleaseDetailUiState> = libraryRepository.observeReleaseTracks(releaseId)
        .flatMapLatest { tracks ->
            val ids = tracks.map { it.blobId }
            val downloadsFlow = if (ids.isEmpty()) flowOf(emptyMap()) else downloadRepository.observeMany(ids)
            combine(
                libraryRepository.observeRelease(releaseId),
                downloadsFlow,
            ) { release, downloads ->
                ReleaseDetailUiState(
                    release = release,
                    tracks = tracks,
                    downloads = downloads,
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = ReleaseDetailUiState(),
        )

    fun playFromIndex(index: Int) {
        val tracks = state.value.tracks
        if (index !in tracks.indices) return
        viewModelScope.launch {
            val queue = tracks.map { it.toPlayable() }
            playbackController.playQueue(queue = queue, startIndex = index)
        }
    }

    /**
     * Действие на иконку скачивания: само определяет, ставить ли в очередь,
     * отменять или удалять — по текущему статусу.
     */
    fun onDownloadAction(track: Track) {
        viewModelScope.launch {
            when (state.value.downloads[track.blobId]?.status) {
                null -> downloadRepository.enqueue(track)
                DownloadStatus.Queued, DownloadStatus.Running -> downloadRepository.cancel(track.blobId)
                DownloadStatus.Completed -> downloadRepository.delete(track.blobId)
                DownloadStatus.Failed -> downloadRepository.enqueue(track)
            }
        }
    }

    private suspend fun Track.toPlayable(): PlayableTrack {
        // Если файл уже на устройстве — играем его, иначе стримим.
        val localFile = downloadRepository.localFile(blobId)
        val url = localFile?.let { Uri.fromFile(it).toString() }
            ?: MediaUrls.trackStreamUrl(blobId = blobId, variant = null)

        return PlayableTrack(
            blobId = blobId,
            title = title,
            artistName = artistName,
            albumName = albumName,
            coverId = coverId,
            durationSeconds = duration,
            streamUrl = url,
        )
    }

    companion object {
        const val ARG_RELEASE_ID = "releaseId"
    }
}
