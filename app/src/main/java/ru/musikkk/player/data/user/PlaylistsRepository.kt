package ru.musikkk.player.data.user

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.musikkk.player.core.network.api.UserDataApi
import ru.musikkk.player.core.network.dto.CreatePlaylistRequest
import ru.musikkk.player.core.network.dto.PlaylistDto
import ru.musikkk.player.core.network.dto.UpdatePlaylistRequest
import ru.musikkk.player.domain.user.Playlist

@Singleton
class PlaylistsRepository @Inject constructor(
    private val api: UserDataApi,
) {
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    suspend fun refresh() {
        val resp = api.getPlaylists()
        _playlists.value = resp.playlists.map { it.toDomain() }
    }

    suspend fun create(name: String): Playlist {
        val resp = api.createPlaylist(CreatePlaylistRequest(name = name))
        val created = resp.playlist.toDomain()
        _playlists.update { listOf(created) + it }
        return created
    }

    /**
     * Добавляет трек в плейлист (если ещё не там) и пишет обновление на сервер.
     * Сервер хранит треки списком rel_path; мы посылаем обновлённый список целиком.
     */
    suspend fun addTrack(playlistId: String, trackPath: String): Playlist {
        val current = _playlists.value.firstOrNull { it.id == playlistId }
            ?: error("playlist $playlistId not found")
        if (trackPath in current.trackPaths) return current

        val newTracks = current.trackPaths + trackPath
        val resp = api.updatePlaylist(
            id = playlistId,
            body = UpdatePlaylistRequest(tracks = newTracks),
        )
        val updated = resp.playlist.toDomain()
        _playlists.update { list -> list.map { if (it.id == playlistId) updated else it } }
        return updated
    }

    suspend fun removeTrack(playlistId: String, trackPath: String): Playlist {
        val current = _playlists.value.firstOrNull { it.id == playlistId }
            ?: error("playlist $playlistId not found")
        if (trackPath !in current.trackPaths) return current

        val newTracks = current.trackPaths.filterNot { it == trackPath }
        val resp = api.updatePlaylist(
            id = playlistId,
            body = UpdatePlaylistRequest(tracks = newTracks),
        )
        val updated = resp.playlist.toDomain()
        _playlists.update { list -> list.map { if (it.id == playlistId) updated else it } }
        return updated
    }

    suspend fun rename(playlistId: String, name: String): Playlist {
        val resp = api.updatePlaylist(
            id = playlistId,
            body = UpdatePlaylistRequest(name = name),
        )
        val updated = resp.playlist.toDomain()
        _playlists.update { list -> list.map { if (it.id == playlistId) updated else it } }
        return updated
    }

    suspend fun delete(playlistId: String) {
        api.deletePlaylist(playlistId)
        _playlists.update { list -> list.filterNot { it.id == playlistId } }
    }

    private fun PlaylistDto.toDomain(): Playlist = Playlist(
        id = id,
        name = name,
        coverId = cover,
        trackPaths = tracks,
        createdAtMs = createdAt,
        updatedAtMs = updatedAt,
    )
}
