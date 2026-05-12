package ru.musikkk.player.data.upload

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.musikkk.player.domain.upload.UploadStatus

class UploadStatusMapperTest {

    @Test
    fun `round-trip всех enum значений`() {
        for (status in UploadStatus.entries) {
            val raw = UploadStatusMapper.toRaw(status)
            val back = UploadStatusMapper.fromRaw(raw)
            assertEquals(status, back)
        }
    }

    @Test
    fun `неизвестное значение трактуем как Queued`() {
        assertEquals(UploadStatus.Queued, UploadStatusMapper.fromRaw(null))
        assertEquals(UploadStatus.Queued, UploadStatusMapper.fromRaw(""))
        assertEquals(UploadStatus.Queued, UploadStatusMapper.fromRaw("UNKNOWN"))
    }
}
