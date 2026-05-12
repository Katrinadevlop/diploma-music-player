package ru.musikkk.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.musikkk.player.core.datastore.TokenStore

@HiltAndroidApp
class MusikkkApp : Application(), ImageLoaderFactory {

    @Inject lateinit var tokenStore: TokenStore

    @Inject lateinit var imageLoaderProvider: dagger.Lazy<ImageLoader>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Прогреваем in-memory кэш токена, чтобы AuthInterceptor OkHttp мог
        // прочитать его синхронно на самом первом сетевом запросе.
        appScope.launch { tokenStore.prime() }
    }

    /**
     * Coil подхватывает этот ImageLoader как singleton по умолчанию, и все
     * `AsyncImage(...)` без явного `imageLoader = ...` ходят через наш
     * авторизованный OkHttpClient.
     */
    override fun newImageLoader(): ImageLoader = imageLoaderProvider.get()
}
