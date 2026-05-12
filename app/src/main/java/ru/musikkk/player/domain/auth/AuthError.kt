package ru.musikkk.player.domain.auth

/**
 * Доменные ошибки авторизации. Мапперы переводят сырые HTTP- и сетевые
 * ошибки в эти значения, чтобы код фич не зависел от Retrofit-типов.
 */
sealed class AuthError(message: String? = null, cause: Throwable? = null) : Throwable(message, cause) {
    data object InvalidCredentials : AuthError("invalid_credentials")
    data object EmailNotVerified : AuthError("email_not_verified")
    data object RateLimited : AuthError("rate_limited")
    data class Network(override val cause: Throwable) : AuthError("network", cause)
    data class Unknown(override val cause: Throwable? = null, val code: String? = null) :
        AuthError(code ?: "unknown", cause)
}
