package ru.musikkk.player.core.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Общий DataStore для всего, что связано с авторизацией: токен сессии
 * и метаданные ожидающего подтверждения регистрации. Один файл —
 * чтобы и `TokenStore`, и `PendingVerificationStore` оперировали
 * консистентно (без шанса очистить токен, оставив pending в живых).
 */
internal val Context.authDataStore by preferencesDataStore(name = "musikkk_auth")
