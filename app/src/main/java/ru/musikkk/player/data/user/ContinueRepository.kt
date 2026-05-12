package ru.musikkk.player.data.user

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import ru.musikkk.player.core.network.api.UserDataApi
import ru.musikkk.player.core.network.dto.ContinueDto
import ru.musikkk.player.core.network.dto.PutContinueRequest
import ru.musikkk.player.domain.user.ContinueState

/**
 * GET /api/user/continue возвращает либо объект, либо JSON-null. Парсим
 * тело вручную — Retrofit/kotlinx умеют nullable, но не любят `null`
 * как top-level response. Поэтому `UserDataApi.getContinue()` отдаёт
 * сырое `ResponseBody`, а здесь мы аккуратно его читаем.
 */
@Singleton
class ContinueRepository @Inject constructor(
    private val api: UserDataApi,
    private val json: Json,
) {
    private val _state = MutableStateFlow<ContinueState?>(null)
    val state: StateFlow<ContinueState?> = _state.asStateFlow()

    suspend fun refresh() {
        val raw = api.getContinue().string()
        _state.value = parseBody(raw)
    }

    suspend fun save(trackPath: String, timeSeconds: Double) {
        api.putContinue(
            PutContinueRequest(
                trackId = trackPath,
                time = timeSeconds.coerceAtLeast(0.0),
                savedAt = System.currentTimeMillis(),
            )
        )
        _state.value = ContinueState(
            trackPath = trackPath,
            timeSeconds = timeSeconds,
            savedAtMs = System.currentTimeMillis(),
        )
    }

    private fun parseBody(raw: String): ContinueState? {
        if (raw.isBlank()) return null
        val element: JsonElement = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: return null
        if (element is JsonNull) return null
        // Обёртка `{continue: {...}}` тоже может прийти — поддержим оба варианта.
        val obj = when (element) {
            is JsonObject -> if ("track_id" in element) element else (element["continue"] as? JsonObject)
            else -> null
        } ?: return null

        val dto = runCatching { json.decodeFromJsonElement(ContinueDto.serializer(), obj) }
            .getOrNull() ?: return null

        return ContinueState(
            trackPath = dto.trackId,
            timeSeconds = dto.time,
            savedAtMs = dto.savedAt,
        )
    }
}
