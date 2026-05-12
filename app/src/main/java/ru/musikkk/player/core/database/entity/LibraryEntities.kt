package ru.musikkk.player.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarCoverId: String?,
)

@Entity(
    tableName = "releases",
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("artistId"),
        Index("section"),
        Index("releaseDate"),
    ],
)
data class ReleaseEntity(
    @PrimaryKey val id: String,
    val artistId: String,
    val artistName: String,
    val name: String,
    val section: String,
    val releaseDate: String?,
    val year: String?,
    val coverId: String?,
)

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = ReleaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["releaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("releaseId"),
        Index("filePath"),
    ],
)
data class TrackEntity(
    @PrimaryKey val blobId: String,
    val releaseId: String,
    val filePath: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val duration: Int,
    val trackNumber: Int?,
    val coverId: String?,
    val bitrate: Int,
    val quality: String?,
    val year: String?,
    val orderInRelease: Int,
)
