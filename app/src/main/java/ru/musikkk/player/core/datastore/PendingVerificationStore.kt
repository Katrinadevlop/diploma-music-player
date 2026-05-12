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
import kotlinx.coroutines.flow.map

/**
 * Метаданные текущей ожидающей подтверждения регистрации.
 *
 * Поля без пароля — пароль живёт только в памяти (см. [PendingVerificationCreds]).
 * `resendBlockedUntilMs` — момент, до которого кнопка «Отправить ещё раз»
 * заблокирована (cooldown сервера, дефолт 60с).
 */
data class PendingVerification(
    val email: String,
    val username: String,
    val createdAtMs: Long,
    val resendBlockedUntilMs: Long,
)

/**
 * Сохраняет состояние «зарегистрировались, ждём подтверждения почты»
 * между запусками приложения. Если процесс был убит на экране verify —
 * после рестарта `AppViewModel` снова откроет этот экран, и пользователь
 * сможет хотя бы дозапросить письмо. Пароль при этом потерян (только в
 * памяти), поэтому для подтверждения регистрации придётся войти заново.
 */
@Singleton
class PendingVerificationStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val Email = stringPreferencesKey("pending_email")
        val Username = stringPreferencesKey("pending_username")
        val CreatedAt = longPreferencesKey("pending_created_at_ms")
        val ResendBlockedUntil = longPreferencesKey("pending_resend_blocked_until_ms")
    }

    val pendingFlow: Flow<PendingVerification?> = context.authDataStore.data
        .map { prefs -> readFromPrefs(prefs) }
        .distinctUntilChanged()

    private fun readFromPrefs(
        prefs: androidx.datastore.preferences.core.Preferences,
    ): PendingVerification? {
        val email = prefs[Keys.Email]?.takeIf { it.isNotBlank() } ?: return null
        val username = prefs[Keys.Username]?.takeIf { it.isNotBlank() } ?: return null
        val createdAt = prefs[Keys.CreatedAt] ?: return null
        val resendUntil = prefs[Keys.ResendBlockedUntil] ?: 0L
        return PendingVerification(
            email = email,
            username = username,
            createdAtMs = createdAt,
            resendBlockedUntilMs = resendUntil,
        )
    }

    suspend fun set(
        email: String,
        username: String,
        retryAfterSeconds: Int?,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.Email] = email
            prefs[Keys.Username] = username
            prefs[Keys.CreatedAt] = nowMs
            prefs[Keys.ResendBlockedUntil] = nowMs + (retryAfterSeconds?.coerceAtLeast(0) ?: 0) * 1000L
        }
    }

    suspend fun updateResendCooldown(
        retryAfterSeconds: Int,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.ResendBlockedUntil] = nowMs + retryAfterSeconds.coerceAtLeast(0) * 1000L
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.Email)
            prefs.remove(Keys.Username)
            prefs.remove(Keys.CreatedAt)
            prefs.remove(Keys.ResendBlockedUntil)
        }
    }
}
