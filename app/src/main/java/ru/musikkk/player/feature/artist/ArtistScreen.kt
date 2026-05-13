package ru.musikkk.player.feature.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.musikkk.player.R
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.ui.components.CoverImage
import ru.musikkk.player.ui.components.PlaybackAwareBackdrop
import ru.musikkk.player.ui.components.ReleaseCard
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

/**
 * Экран артиста — повторяет `view-artist` веб-клиента:
 *   * шапка с круглым аватаром и именем;
 *   * четыре секции релизов: Альбомы / EP / Синглы / Коллаборации
 *     (пустые секции не рендерим).
 *
 * Используем один [LazyVerticalGrid] с заголовками через `GridItemSpan(maxLineSpan)`
 * — это идиоматичный Compose-паттерн для multi-section grid'ов
 * (вложенные LazyGrid внутри LazyColumn устроили бы конфликт скролла).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    onBack: () -> Unit,
    onReleaseClick: (Release) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop — обложка первого релиза артиста или текущего трека.
        PlaybackAwareBackdrop(fallbackCoverId = state.albums.firstOrNull()?.coverId)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.artist?.name.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.release_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
            containerColor = Color.Transparent,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.artist == null -> Text(
                        text = stringResource(id = R.string.artist_not_found),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> ArtistContent(
                        state = state,
                        onReleaseClick = onReleaseClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistContent(
    state: ArtistUiState,
    onReleaseClick: (Release) -> Unit,
) {
    val totalReleases = state.albums.size + state.eps.size + state.singles.size + state.collabs.size

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MusikkkSpacing.s4,
            end = MusikkkSpacing.s4,
            top = MusikkkSpacing.s2,
            bottom = MusikkkSpacing.s6,
        ),
        horizontalArrangement = Arrangement.spacedBy(MusikkkSpacing.s3),
        verticalArrangement = Arrangement.spacedBy(MusikkkSpacing.s4),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
            ArtistHeader(name = state.artist?.name.orEmpty(), avatarCoverId = state.artist?.avatarCoverId)
        }

        if (totalReleases == 0) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                Text(
                    text = stringResource(id = R.string.artist_no_releases),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = MusikkkSpacing.s6),
                )
            }
            return@LazyVerticalGrid
        }

        section(
            titleRes = R.string.library_section_albums,
            releases = state.albums,
            onReleaseClick = onReleaseClick,
        )
        section(
            titleRes = R.string.library_section_eps,
            releases = state.eps,
            onReleaseClick = onReleaseClick,
        )
        section(
            titleRes = R.string.library_section_singles,
            releases = state.singles,
            onReleaseClick = onReleaseClick,
        )
        section(
            titleRes = R.string.library_section_collabs,
            releases = state.collabs,
            onReleaseClick = onReleaseClick,
        )
    }
}

/**
 * DSL-расширение для LazyGridScope: добавляет заголовок секции на всю
 * ширину и сетку карточек релизов под ним. Пустые секции пропускаются.
 */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.section(
    titleRes: Int,
    releases: List<Release>,
    onReleaseClick: (Release) -> Unit,
) {
    if (releases.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }, key = "title-$titleRes") {
        SectionTitle(titleRes = titleRes)
    }
    items(releases, key = { "rel-${it.id}" }) { release ->
        ReleaseCard(
            release = release,
            onClick = { onReleaseClick(release) },
            showArtistName = false,
        )
    }
}

@Composable
private fun ArtistHeader(name: String, avatarCoverId: String?) {
    Column(
        modifier = Modifier
            .padding(vertical = MusikkkSpacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverImage(
            coverId = avatarCoverId,
            contentDescription = name,
            modifier = Modifier.size(160.dp),
            fallbackText = name,
            radius = MusikkkRadius.pill,
        )
        Spacer(Modifier.height(MusikkkSpacing.s4))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionTitle(titleRes: Int) {
    Text(
        text = stringResource(id = titleRes),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = MusikkkSpacing.s4, bottom = MusikkkSpacing.s2),
    )
}
