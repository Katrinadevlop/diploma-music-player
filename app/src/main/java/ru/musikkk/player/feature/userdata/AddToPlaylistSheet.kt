package ru.musikkk.player.feature.userdata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.musikkk.player.R
import ru.musikkk.player.data.user.PlaylistsRepository
import ru.musikkk.player.domain.user.Playlist
import ru.musikkk.player.ui.components.MusikkkTextField
import ru.musikkk.player.ui.theme.MusikkkSpacing
import ru.musikkk.player.ui.util.tracksCountString

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val playlistsRepository: PlaylistsRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistsRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    init {
        viewModelScope.launch { runCatching { playlistsRepository.refresh() } }
    }

    fun addToExisting(playlistId: String, trackPath: String) {
        viewModelScope.launch {
            runCatching { playlistsRepository.addTrack(playlistId, trackPath) }
        }
    }

    fun createAndAdd(name: String, trackPath: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val created = playlistsRepository.create(name.trim())
                playlistsRepository.addTrack(created.id, trackPath)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    trackPath: String,
    onDismiss: () -> Unit,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var newName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = MusikkkSpacing.s4)) {
            Text(
                text = stringResource(id = R.string.playlist_add_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    horizontal = MusikkkSpacing.s5,
                    vertical = MusikkkSpacing.s3,
                ),
            )

            if (playlists.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playlists, key = { it.id }) { pl ->
                        PlaylistPickerRow(
                            playlist = pl,
                            onClick = {
                                viewModel.addToExisting(pl.id, trackPath)
                                onDismiss()
                            },
                        )
                    }
                }
                Spacer(Modifier.height(MusikkkSpacing.s3))
            }

            Text(
                text = stringResource(id = R.string.playlist_add_new_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = MusikkkSpacing.s5,
                    vertical = MusikkkSpacing.s2,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MusikkkSpacing.s5),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MusikkkSpacing.s3),
            ) {
                MusikkkTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = stringResource(id = R.string.playlists_name_label),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        viewModel.createAndAdd(newName, trackPath)
                        newName = ""
                        onDismiss()
                    },
                    enabled = newName.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.playlists_create),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistPickerRow(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(MusikkkSpacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = tracksCountString(playlist.trackPaths.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
