package ru.musikkk.player.feature.auth

import androidx.annotation.StringRes
import ru.musikkk.player.R
import ru.musikkk.player.domain.auth.AuthError

/** Переводит доменные ошибки авторизации в string-ресурсы для UI. */
@StringRes
fun AuthError.toMessageRes(): Int = when (this) {
    AuthError.InvalidCredentials -> R.string.auth_error_invalid_credentials
    AuthError.EmailNotVerified -> R.string.auth_error_email_not_verified
    AuthError.UsernameInvalid -> R.string.register_error_username_invalid
    AuthError.UsernameTaken -> R.string.register_error_username_taken
    AuthError.PasswordTooShort -> R.string.register_error_password_too_short
    AuthError.PasswordInvalid -> R.string.register_error_password_invalid
    AuthError.EmailInvalid -> R.string.register_error_email_invalid
    AuthError.EmailTaken -> R.string.register_error_email_taken
    AuthError.EmailRequired -> R.string.register_error_email_required
    AuthError.TermsRequired -> R.string.register_error_terms_required
    is AuthError.ResendTooSoon -> R.string.auth_error_rate_limited
    is AuthError.RateLimited -> R.string.auth_error_rate_limited
    is AuthError.Network -> R.string.auth_error_network
    is AuthError.Unknown -> R.string.auth_error_unknown
}
