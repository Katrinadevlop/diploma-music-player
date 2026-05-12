package ru.musikkk.player.data.user

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import ru.musikkk.player.core.media.NetworkQualityResolver
import ru.musikkk.player.core.network.MediaUrls
import ru.musikkk.player.data.download.DownloadRepository
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.data.playback.PlaybackController
import ru.musikkk.player.data.settings.SettingsRepository
import ru.musikkk.player.domain.library.Track
import ru.musikkk.player.domain.playback.PlayableTrack

/**
 * Use case: получить из набора `rel_path` плеер-очередь, учесть скачанные
 * локально файлы и пользовательское качество стрима, и запустить
 * воспроизведение с указанного индекса.
 *
 * Используется на всех «вторичных» экранах (Liked / Playlist detail /
 * Recent / Top) — там, где нужно «нажал на трек → играй».
 */
@Singleton
class PlayPathsAction @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: DownloadRepository,
    private val playbackController: PlaybackController,
    private val settingsRepository: SettingsRepository,
    private val networkQualityResolver: NetworkQualityResolver,
) {
    suspend operator fun invoke(
        paths: List<String>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L,
    ) {
        if (paths.isEmpty()) return
        val tracks = libraryRepository.tracksByPaths(paths)
        if (tracks.isEmpty()) return

        val streamQuality = settingsRepository.settingsFlow.first().streamQuality
        val variant = networkQualityResolver.preferredVariant(streamQuality)

        val queue = tracks.map { it.toPlayable(variant) }
        val safeStart = startIndex.coerceIn(0, queue.lastIndex)
        playbackController.playQueue(
            queue = queue,
            startIndex = safeStart,
            startPositionMs = startPositionMs,
        )
    }

    private suspend fun Track.toPlayable(variant: String?): PlayableTrack {
        val localFile = downloadRepository.localFile(blobId)
        val url = localFile?.let { Uri.fromFile(it).toString() }
            ?: MediaUrls.trackStreamUrl(blobId = blobId, variant = variant)
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
}
