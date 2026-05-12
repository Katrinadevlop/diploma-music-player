package ru.musikkk.player

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.musikkk.player.core.datastore.TokenStore

@HiltAndroidApp
class MusikkkApp : Application() {

    @Inject lateinit var tokenStore: TokenStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Прогреваем in-memory кэш токена, чтобы AuthInterceptor OkHttp мог
        // прочитать его синхронно на самом первом сетевом запросе.
        appScope.launch { tokenStore.prime() }
    }
}
