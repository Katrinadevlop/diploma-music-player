package ru.musikkk.player.data.playback

import androidx.media3.common.Player
import ru.musikkk.player.domain.playback.RepeatMode

/** Конвертация доменного `RepeatMode` ↔ `Player.REPEAT_MODE_*` Media3. */
internal object RepeatModeMapper {

    fun fromPlayer(value: Int): RepeatMode = when (value) {
        Player.REPEAT_MODE_ONE -> RepeatMode.One
        Player.REPEAT_MODE_ALL -> RepeatMode.All
        else -> RepeatMode.Off
    }

    fun toPlayer(mode: RepeatMode): Int = when (mode) {
        RepeatMode.Off -> Player.REPEAT_MODE_OFF
        RepeatMode.One -> Player.REPEAT_MODE_ONE
        RepeatMode.All -> Player.REPEAT_MODE_ALL
    }
}
