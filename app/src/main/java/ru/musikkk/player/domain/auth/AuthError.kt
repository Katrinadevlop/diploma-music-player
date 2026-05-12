package ru.musikkk.player.domain.auth

/**
 * Доменные ошибки авторизации. Мапперы переводят сырые HTTP- и сетевые
 * ошибки в эти значения, чтобы код фич не зависел от Retrofit-типов.
 *
 * `retryAfterSeconds` появляется на rate-limit и cooldown ответах сервера
 * и позволяет UI выставить корректный обратный отсчёт.
 */
sealed class AuthError(message: String? = null, cause: Throwable? = null) : Throwable(message, cause) {
    // Login
    data object InvalidCredentials : AuthError("invalid_credentials")
    data object EmailNotVerified : AuthError("email_not_verified")

    // Register (field-level)
    data object UsernameInvalid : AuthError("username_invalid")
    data object UsernameTaken : AuthError("username_taken")
    data object PasswordTooShort : AuthError("password_too_short")
    data object PasswordInvalid : AuthError("password_invalid")
    data object EmailInvalid : AuthError("email_invalid")
    data object EmailTaken : AuthError("email_taken")
    data object EmailRequired : AuthError("email_required")
    data object TermsRequired : AuthError("terms_privacy_required")

    // Verify resend
    data class ResendTooSoon(val retryAfterSeconds: Int) : AuthError("resend_too_soon")

    // Общие
    data class RateLimited(val retryAfterSeconds: Int? = null) : AuthError("rate_limited")
    data class Network(override val cause: Throwable) : AuthError("network", cause)
    data class Unknown(override val cause: Throwable? = null, val code: String? = null) :
        AuthError(code ?: "unknown", cause)
}
