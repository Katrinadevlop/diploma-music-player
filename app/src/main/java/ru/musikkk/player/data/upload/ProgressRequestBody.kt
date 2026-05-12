package ru.musikkk.player.data.upload

import java.io.InputStream
import java.util.concurrent.atomic.AtomicLong
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink

/**
 * `RequestBody`, который стримит из `InputStream`-фабрики и параллельно
 * считает уже переданные байты в `bytesWritten`. Фабрика нужна, потому
 * что OkHttp при сетевой ошибке может попробовать `writeTo` повторно,
 * а `InputStream` обычно одноразовый — на повторе пересоздадим его.
 */
internal class ProgressRequestBody(
    private val source: () -> InputStream,
    private val contentType: MediaType?,
    private val contentLength: Long,
    private val bytesWritten: AtomicLong,
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = if (contentLength > 0) contentLength else -1L

    override fun writeTo(sink: BufferedSink) {
        // На случай retry — сбрасываем счётчик в начале каждой записи.
        bytesWritten.set(0)
        source().use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                sink.write(buffer, 0, read)
                bytesWritten.addAndGet(read.toLong())
            }
        }
    }

    private companion object {
        const val BUFFER_BYTES = 64 * 1024
    }
}
