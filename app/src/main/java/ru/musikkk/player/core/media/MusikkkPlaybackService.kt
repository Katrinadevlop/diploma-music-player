package ru.musikkk.player.core.media

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Фоновый сервис воспроизведения. Реализует контракт `MediaSessionService` —
 * фреймворк сам поднимает foreground-нотификацию с управлением, пускает
 * клавиши с локскрина/Bluetooth-гарнитуры и поддерживает воспроизведение
 * при свёрнутом приложении.
 *
 * UI общается с сервисом через `MediaController` (см. `PlaybackController`),
 * сам сервис не имеет прямого Compose-API.
 */
@AndroidEntryPoint
class MusikkkPlaybackService : MediaSessionService() {

    @Inject lateinit var playerFactory: PlayerFactory

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = playerFactory.create()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Если пользователь свайпнул приложение из недавних — останавливаем
        // воспроизведение и отпускаем сервис. Без этого нотификация
        // продолжит висеть, что для музыкального плеера неудобно (стандарт
        // ОС: «убрал из недавних — остановилось»).
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
