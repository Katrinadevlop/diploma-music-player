package ru.musikkk.player.data.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Где живут скачанные аудио-файлы. Один файл на blob_id, имя — это сам
 * blob_id с расширением `.bin` (формат остаётся в `mime_type` БД,
 * ExoPlayer и так определяет тип по содержимому/Content-Type).
 *
 * Корень — `filesDir/tracks/` приложения. Файлы автоматически удаляются
 * вместе с приложением (т. е. при `clearData`), но не идут в backup.
 */
@Singleton
class DownloadFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val root: File by lazy { File(context.filesDir, "tracks").apply { mkdirs() } }

    fun fileNameFor(blobId: String): String = "$blobId.bin"

    fun fileFor(blobId: String): File = File(root, fileNameFor(blobId))
}
