package ru.musikkk.player.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранит пароль для ожидающей подтверждения регистрации **только в памяти**.
 *
 * После завершения регистрации пользователь попадает на экран verify-email;
 * если он подтвердит почту в этой же сессии и нажмёт «Я подтвердил» —
 * мы используем сохранённые `username/password` чтобы получить токен без
 * повторного ввода. Если процесс убили — пароль теряется, и
 * пользователю придётся войти на экране Login (а pending-метаданные
 * восстановятся из DataStore через [ru.musikkk.player.core.datastore.PendingVerificationStore]).
 */
@Singleton
class PendingVerificationCreds @Inject constructor() {

    @Volatile
    private var snapshot: Credentials? = null

    fun set(username: String, password: String) {
        snapshot = Credentials(username = username, password = password)
    }

    fun read(): Credentials? = snapshot

    fun clear() {
        snapshot = null
    }

    data class Credentials(
        val username: String,
        val password: String,
    )
}
