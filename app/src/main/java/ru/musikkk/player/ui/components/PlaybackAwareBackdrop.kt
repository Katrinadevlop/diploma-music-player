package ru.musikkk.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.musikkk.player.feature.player.PlaybackViewModel

/**
 * Backdrop, который автоматически подтягивает обложку текущего играющего
 * трека. Используется на «нейтральных» экранах — Library, Hub,
 * Liked/Playlists/Recent/Top — где нет своей доминирующей обложки.
 *
 * Когда плеер пустой, используем `fallbackCoverId` (например, первая
 * обложка из списка треков на экране), а если и его нет — рендерим
 * градиентный плейсхолдер из [MusikkkBackdrop].
 */
@Composable
fun PlaybackAwareBackdrop(
    fallbackCoverId: String? = null,
    modifier: Modifier = Modifier,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
) {
    val state by playbackViewModel.state.collectAsStateWithLifecycle()
    val coverId = state.currentTrack?.coverId ?: fallbackCoverId
    MusikkkBackdrop(coverId = coverId, modifier = modifier)
}
