package ru.musikkk.player.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.musikkk.player.core.network.dto.ArtistDto
import ru.musikkk.player.core.network.dto.LibraryResponseDto
import ru.musikkk.player.core.network.dto.ReleaseDto
import ru.musikkk.player.core.network.dto.TrackDto

class LibraryMappersTest {

    @Test
    fun `пустой ответ даёт пустой snapshot`() {
        val snapshot = LibraryResponseDto().toSnapshot()
        assertEquals(0, snapshot.artists.size)
        assertEquals(0, snapshot.releases.size)
        assertEquals(0, snapshot.tracks.size)
    }

    @Test
    fun `один артист с альбомом и синглом раскладывается в три таблицы`() {
        val response = LibraryResponseDto(
            artists = listOf(
                ArtistDto(
                    id = "Aphex Twin",
                    name = "Aphex Twin",
                    avatar = "cover-avatar",
                    albums = listOf(
                        ReleaseDto(
                            id = "Selected Ambient Works",
                            name = "Selected Ambient Works",
                            releaseDate = "1992-11-09",
                            cover = "cover-album",
                            tracks = listOf(
                                track("blob-1", "Xtal", trackNumber = "1", duration = 290),
                                track("blob-2", "Tha", trackNumber = "2", duration = 540),
                            ),
                        ),
                    ),
                    singles = listOf(
                        ReleaseDto(
                            id = "Avril 14th",
                            name = "Avril 14th",
                            releaseDate = "2001-10-22",
                            cover = null,
                            tracks = listOf(
                                track("blob-3", "Avril 14th", trackNumber = "1", duration = 125),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val snapshot = response.toSnapshot()

        assertEquals(1, snapshot.artists.size)
        assertEquals("Aphex Twin", snapshot.artists.single().id)
        assertEquals("cover-avatar", snapshot.artists.single().avatarCoverId)

        assertEquals(2, snapshot.releases.size)

        val album = snapshot.releases.first { it.section == "albums" }
        assertEquals("Aphex Twin|albums|Selected Ambient Works", album.id)
        assertEquals("Aphex Twin", album.artistId)
        assertEquals("Selected Ambient Works", album.name)
        assertEquals("1992-11-09", album.releaseDate)
        assertEquals("cover-album", album.coverId)

        val single = snapshot.releases.first { it.section == "singles" }
        assertEquals("Aphex Twin|singles|Avril 14th", single.id)
        assertNull(single.coverId)

        assertEquals(3, snapshot.tracks.size)
        val albumTracks = snapshot.tracks.filter { it.releaseId == album.id }
        assertEquals(listOf(0, 1), albumTracks.map { it.orderInRelease })
        assertEquals(listOf(1, 2), albumTracks.map { it.trackNumber })
    }

    @Test
    fun `одинаковые имена релизов в разных секциях не схлопываются`() {
        val response = LibraryResponseDto(
            artists = listOf(
                ArtistDto(
                    id = "Artist",
                    name = "Artist",
                    albums = listOf(
                        ReleaseDto(
                            id = "Untitled",
                            name = "Untitled",
                            tracks = listOf(track("a", "T1")),
                        ),
                    ),
                    eps = listOf(
                        ReleaseDto(
                            id = "Untitled",
                            name = "Untitled",
                            tracks = listOf(track("b", "T2")),
                        ),
                    ),
                ),
            ),
        )

        val ids = response.toSnapshot().releases.map { it.id }
        assertEquals(2, ids.toSet().size)
        assertEquals(
            setOf("Artist|albums|Untitled", "Artist|eps|Untitled"),
            ids.toSet(),
        )
    }

    @Test
    fun `год релиза берётся из первого трека с заполненным year`() {
        val response = LibraryResponseDto(
            artists = listOf(
                ArtistDto(
                    id = "A",
                    name = "A",
                    albums = listOf(
                        ReleaseDto(
                            id = "R",
                            name = "R",
                            tracks = listOf(
                                track("x", "t1", year = null),
                                track("y", "t2", year = "2019"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val release = response.toSnapshot().releases.single()
        assertEquals("2019", release.year)
    }

    private fun track(
        blobId: String,
        title: String,
        trackNumber: String? = null,
        duration: Int = 180,
        year: String? = null,
    ): TrackDto = TrackDto(
        blobId = blobId,
        filePath = "$blobId.flac",
        artist = "A",
        album = "R",
        title = title,
        trackNumber = trackNumber,
        duration = duration,
        year = year,
    )
}
