package ru.musikkk.player.feature.auth

import androidx.annotation.StringRes
import ru.musikkk.player.R
import ru.musikkk.player.domain.auth.AuthError

/** Переводит доменные ошибки авторизации в string-ресурсы для UI. */
@StringRes
fun AuthError.toMessageRes(): Int = when (this) {
    AuthError.InvalidCredentials -> R.string.auth_error_invalid_credentials
    AuthError.EmailNotVerified -> R.string.auth_error_email_not_verified
    AuthError.RateLimited -> R.string.auth_error_rate_limited
    is AuthError.Network -> R.string.auth_error_network
    is AuthError.Unknown -> R.string.auth_error_unknown
}
