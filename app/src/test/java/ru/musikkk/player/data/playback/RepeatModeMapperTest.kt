package ru.musikkk.player.data.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.musikkk.player.domain.playback.RepeatMode

class RepeatModeMapperTest {

    @Test
    fun `fromPlayer мапит все Player константы`() {
        assertEquals(RepeatMode.Off, RepeatModeMapper.fromPlayer(Player.REPEAT_MODE_OFF))
        assertEquals(RepeatMode.One, RepeatModeMapper.fromPlayer(Player.REPEAT_MODE_ONE))
        assertEquals(RepeatMode.All, RepeatModeMapper.fromPlayer(Player.REPEAT_MODE_ALL))
    }

    @Test
    fun `fromPlayer неизвестное значение - Off`() {
        assertEquals(RepeatMode.Off, RepeatModeMapper.fromPlayer(42))
        assertEquals(RepeatMode.Off, RepeatModeMapper.fromPlayer(-1))
    }

    @Test
    fun `toPlayer соответствует обратно`() {
        for (mode in RepeatMode.entries) {
            val roundTrip = RepeatModeMapper.fromPlayer(RepeatModeMapper.toPlayer(mode))
            assertEquals(mode, roundTrip)
        }
    }
}
