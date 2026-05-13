package ru.musikkk.player.core.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import okhttp3.OkHttpClient
import ru.musikkk.player.BuildConfig
import ru.musikkk.player.core.network.di.AuthClient

/**
 * Собирает `ExoPlayer`, привязанный к нашему `@AuthClient` OkHttp —
 * `AuthInterceptor` автоматически подставляет `Authorization: Bearer`
 * в каждый запрос за сегментом аудио.
 *
 * Live-инстанс плеера живёт в [MusikkkPlaybackService]. Фабрика
 * нужна, чтобы DI собрал его внутри `@AndroidEntryPoint`-сервиса.
 */
class PlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    @AuthClient private val okHttpClient: OkHttpClient,
) {
    fun create(): ExoPlayer {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Musikkk-Android/${BuildConfig.VERSION_NAME}")

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            // Запросить focus при play + понижать громкость при duck.
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            // Авто-пауза, когда наушники выдернули или подключилась колонка с другим стримом.
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
