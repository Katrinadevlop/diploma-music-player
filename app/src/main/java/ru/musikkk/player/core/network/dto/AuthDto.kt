package ru.musikkk.player.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenRequest(
    val username: String,
    val password: String,
)

@Serializable
data class TokenResponse(
    val ok: Boolean = true,
    val token: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_at") val expiresAt: Long,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
)

@Serializable
data class MeResponse(
    val ok: Boolean = true,
    val user: UserDto? = null,
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    @SerialName("consent_terms_privacy") val consentTermsPrivacy: Boolean = true,
    @SerialName("consent_email_marketing") val consentEmailMarketing: Boolean = false,
)

@Serializable
data class RegisterResponse(
    val ok: Boolean = true,
    @SerialName("needs_email_verification") val needsEmailVerification: Boolean = false,
    @SerialName("retry_after") val retryAfter: Int? = null,
    val user: UserDto? = null,
)

@Serializable
data class ApiError(
    val error: String,
    val message: String? = null,
)
