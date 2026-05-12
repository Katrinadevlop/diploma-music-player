package ru.musikkk.player.domain.auth

data class AuthSession(
    val token: String,
    val expiresAtMs: Long,
    val user: AuthUser,
)

data class AuthUser(
    val publicId: String,
    val username: String,
)
