package ru.musikkk.player.core.network

import ru.musikkk.player.BuildConfig

/**
 * Утилиты сборки URL для медиа-эндпоинтов. Базовый URL берём из
 * `BuildConfig.API_BASE_URL`, чтобы не дублировать константу.
 */
object MediaUrls {

    /**
     * URL обложки по `cover_blob_id`. Возвращает `null`, если у трека/релиза
     * обложки нет — UI покажет плейсхолдер.
     */
    fun coverUrl(coverId: String?): String? {
        val id = coverId?.takeIf { it.isNotBlank() } ?: return null
        return BuildConfig.API_BASE_URL.trimEnd('/') + "/api/cover/" + id
    }

    /** URL аудио-стрима по `blob_id`. Опциональный `variant` — например, `aac_128`. */
    fun trackStreamUrl(blobId: String, variant: String? = null): String {
        val base = BuildConfig.API_BASE_URL.trimEnd('/') + "/api/track/" + blobId
        return if (variant.isNullOrBlank()) base else "$base?v=$variant"
    }
}
