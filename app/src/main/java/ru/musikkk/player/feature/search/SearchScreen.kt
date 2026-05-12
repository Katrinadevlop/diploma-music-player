package ru.musikkk.player.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.musikkk.player.R
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.Track
import ru.musikkk.player.ui.components.CoverImage
import ru.musikkk.player.ui.format.formatDuration
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onReleaseClick: (Release) -> Unit,
    onTrackClick: (Track) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // На вход в экран сразу ставим фокус на поле и поднимаем клавиатуру.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SearchField(
                        value = state.query,
                        onChange = viewModel::onQueryChange,
                        onClear = viewModel::clear,
                        focusRequester = focusRequester,
                        onImeDone = { keyboard?.hide() },
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
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                !state.hasQuery -> Hint(textRes = R.string.search_empty_initial)
                state.isEmpty -> Hint(textRes = R.string.search_empty_no_results)
                else -> Results(
                    releases = state.releases,
                    tracks = state.tracks,
                    onReleaseClick = onReleaseClick,
                    onTrackClick = onTrackClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    onImeDone: () -> Unit,
) {
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(stringResource(id = R.string.search_hint)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.search_clear),
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun Hint(textRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MusikkkSpacing.s5),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = textRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Results(
    releases: List<Release>,
    tracks: List<Track>,
    onReleaseClick: (Release) -> Unit,
    onTrackClick: (Track) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = MusikkkSpacing.s3),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (releases.isNotEmpty()) {
            item(key = "header_releases") {
                SectionHeader(stringResource(id = R.string.search_section_releases))
            }
            items(releases, key = { "rel:${it.id}" }) { release ->
                ReleaseRow(release = release, onClick = { onReleaseClick(release) })
            }
        }
        if (tracks.isNotEmpty()) {
            item(key = "header_tracks") {
                SectionHeader(stringResource(id = R.string.search_section_tracks))
            }
            items(tracks, key = { "tr:${it.blobId}" }) { track ->
                TrackRow(track = track, onClick = { onTrackClick(track) })
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = MusikkkSpacing.s5,
            vertical = MusikkkSpacing.s3,
        ),
    )
}

@Composable
private fun ReleaseRow(release: Release, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverId = release.coverId,
            contentDescription = release.name,
            modifier = Modifier.size(48.dp),
            fallbackText = release.name,
            radius = MusikkkRadius.sm,
        )
        Spacer(Modifier.width(MusikkkSpacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = release.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = release.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverId = track.coverId,
            contentDescription = track.title,
            modifier = Modifier.size(48.dp),
            fallbackText = track.title,
            radius = MusikkkRadius.sm,
        )
        Spacer(Modifier.width(MusikkkSpacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(track.artistName, track.albumName).distinct().joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(MusikkkSpacing.s2))
        Text(
            text = formatDuration(track.duration),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
