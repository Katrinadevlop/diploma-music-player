package ru.musikkk.player.data.download

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.musikkk.player.domain.download.DownloadStatus

class DownloadStatusMapperTest {

    @Test
    fun `round-trip всех enum значений`() {
        for (status in DownloadStatus.entries) {
            val raw = DownloadStatusMapper.toRaw(status)
            val back = DownloadStatusMapper.fromRaw(raw)
            assertEquals(status, back)
        }
    }

    @Test
    fun `неизвестное значение трактуем как Queued`() {
        assertEquals(DownloadStatus.Queued, DownloadStatusMapper.fromRaw(null))
        assertEquals(DownloadStatus.Queued, DownloadStatusMapper.fromRaw(""))
        assertEquals(DownloadStatus.Queued, DownloadStatusMapper.fromRaw("UNKNOWN"))
    }
}
