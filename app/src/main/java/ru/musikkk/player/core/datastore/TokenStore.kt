package ru.musikkk.player.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Хранилище Bearer-токена, выданного `POST /api/auth/token`.
 *
 * Значение токена — тот же непрозрачный session id, который сервер хранит
 * в таблице `sessions`, поэтому относимся к нему как к учётной записи:
 * не логируем и не отдаём в крэш-репорты.
 *
 * Помимо DataStore на диске держим in-memory снэпшот в [cachedToken],
 * чтобы OkHttp-перехватчики могли читать токен синхронно, без `runBlocking`.
 * Снэпшот прогревается один раз на старте приложения (см. [prime]) и
 * остаётся консистентным: [save] и [clear] обновляют его напрямую, а
 * [tokenFlow] подхватывает любые внешние изменения DataStore.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val Token = stringPreferencesKey("session_token")
        val ExpiresAtMs = longPreferencesKey("session_expires_at_ms")
    }

    @Volatile
    private var memoryCache: String? = null

    val tokenFlow: Flow<String?> = context.authDataStore.data
        .map { prefs -> prefs[Keys.Token]?.takeIf { it.isNotBlank() } }
        .onEach { memoryCache = it }
        .distinctUntilChanged()

    /** Синхронный снэпшот для OkHttp-перехватчиков. Возвращает null, пока не отработал [prime]. */
    fun cachedToken(): String? = memoryCache

    /** Однократное чтение, прогревающее [memoryCache] с диска. Вызывать один раз на старте приложения. */
    suspend fun prime() {
        memoryCache = tokenFlow.first()
    }

    suspend fun save(token: String, expiresAtMs: Long) {
        memoryCache = token
        context.authDataStore.edit { prefs ->
            prefs[Keys.Token] = token
            prefs[Keys.ExpiresAtMs] = expiresAtMs
        }
    }

    suspend fun clear() {
        memoryCache = null
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.Token)
            prefs.remove(Keys.ExpiresAtMs)
        }
    }
}
