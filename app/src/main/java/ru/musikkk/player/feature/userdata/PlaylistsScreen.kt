package ru.musikkk.player.feature.userdata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.musikkk.player.R
import ru.musikkk.player.data.user.PlaylistsRepository
import ru.musikkk.player.domain.user.Playlist
import ru.musikkk.player.ui.components.PlaybackAwareBackdrop
import ru.musikkk.player.ui.components.SectionCover
import ru.musikkk.player.ui.theme.MusikkkSpacing

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistsRepository: PlaylistsRepository,
) : ViewModel() {

    val state: StateFlow<PlaylistsUiState> = playlistsRepository.playlists
        .map { PlaylistsUiState(playlists = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = PlaylistsUiState(),
        )

    init {
        viewModelScope.launch { runCatching { playlistsRepository.refresh() } }
    }

    fun create(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { runCatching { playlistsRepository.create(name.trim()) } }
    }

    fun delete(playlistId: String) {
        viewModelScope.launch { runCatching { playlistsRepository.delete(playlistId) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onBack: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
    PlaybackAwareBackdrop()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.hub_playlists)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.release_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.playlists_create),
                )
            }
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.playlists.isEmpty() && !state.isLoading) {
                Text(
                    text = stringResource(id = R.string.playlists_empty),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(MusikkkSpacing.s5),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.playlists, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = { onOpenPlaylist(playlist) },
                            onDelete = { deletingId = playlist.id },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    if (creating) {
        CreatePlaylistDialog(
            onDismiss = { creating = false },
            onConfirm = { name ->
                viewModel.create(name)
                creating = false
            },
        )
    }

    deletingId?.let { id ->
        val pl = state.playlists.firstOrNull { it.id == id }
        if (pl != null) {
            AlertDialog(
                onDismissRequest = { deletingId = null },
                title = { Text(stringResource(id = R.string.playlists_delete_title)) },
                text = { Text(stringResource(id = R.string.playlists_delete_body, pl.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(id)
                        deletingId = null
                    }) {
                        Text(stringResource(id = R.string.playlists_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingId = null }) {
                        Text(stringResource(id = R.string.common_cancel))
                    }
                },
            )
        }
    }
    } // closes outer Box around backdrop
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionCover(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.size(MusikkkSpacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralizeTracksCount(playlist.trackPaths.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(id = R.string.playlists_delete_confirm),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.playlists_create_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(id = R.string.playlists_name_label)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(id = R.string.playlists_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun pluralizeTracksCount(count: Int): String {
    val resId = when {
        count == 0 -> R.string.library_track_count_zero
        count == 1 -> R.string.library_track_count_one
        else -> R.string.library_track_count_other
    }
    return if (resId == R.string.library_track_count_zero) {
        stringResource(id = resId)
    } else {
        stringResource(id = resId, count)
    }
}
