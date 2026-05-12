package ru.musikkk.player

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.musikkk.player.core.datastore.TokenStore

@HiltAndroidApp
class MusikkkApp : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject lateinit var tokenStore: TokenStore

    @Inject lateinit var imageLoaderProvider: Lazy<ImageLoader>

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var playbackTracker: ru.musikkk.player.data.user.PlaybackTracker

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Прогреваем in-memory кэш токена, чтобы AuthInterceptor OkHttp мог
        // прочитать его синхронно на самом первом сетевом запросе.
        appScope.launch { tokenStore.prime() }
        // Запускаем сбор данных по воспроизведению (recent / playcounts / continue).
        playbackTracker.start()
    }

    /**
     * Coil подхватывает этот ImageLoader как singleton по умолчанию, и все
     * `AsyncImage(...)` без явного `imageLoader = ...` ходят через наш
     * авторизованный OkHttpClient.
     */
    override fun newImageLoader(): ImageLoader = imageLoaderProvider.get()

    /**
     * WorkManager инициализируется ленивым `androidx.startup` контракт-провайдером,
     * который читает эту конфигурацию. `HiltWorkerFactory` позволяет нашему
     * `@HiltWorker`-воркеру получать зависимости через DI.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
