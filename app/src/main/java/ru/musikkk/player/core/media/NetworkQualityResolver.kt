package ru.musikkk.player.core.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import ru.musikkk.player.domain.settings.StreamQuality

/**
 * Тип активного сетевого соединения. Нужен для выбора качества стрима
 * при `StreamQuality.Auto`.
 */
enum class NetworkType { WiFi, Cellular, Ethernet, Other, None }

/** Чистая абстракция, чтобы тесты не зависели от [ConnectivityManager]. */
interface NetworkTypeProvider {
    fun current(): NetworkType
}

@Singleton
class AndroidNetworkTypeProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkTypeProvider {

    override fun current(): NetworkType {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkType.None
        val active = cm.activeNetwork ?: return NetworkType.None
        val caps = cm.getNetworkCapabilities(active) ?: return NetworkType.None

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WiFi
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Ethernet
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Cellular
            else -> NetworkType.Other
        }
    }
}

@Singleton
class NetworkQualityResolver @Inject constructor(
    private val networkTypeProvider: NetworkTypeProvider,
) {
    /**
     * Возвращает имя варианта (`aac_128`) или `null` — значит стримим оригинал.
     *
     * Поведение:
     * - [StreamQuality.Original] — всегда `null` (оригинал).
     * - [StreamQuality.Aac128]   — всегда `"aac_128"` (сервер сам fallback'нет
     *   на оригинал, если у трека нет такого варианта).
     * - [StreamQuality.Auto]     — `null` на Wi-Fi/Ethernet, `"aac_128"` на
     *   мобильной сети.
     */
    fun preferredVariant(quality: StreamQuality): String? = when (quality) {
        StreamQuality.Original -> null
        StreamQuality.Aac128 -> AAC_128
        StreamQuality.Auto -> when (networkTypeProvider.current()) {
            NetworkType.WiFi, NetworkType.Ethernet -> null
            NetworkType.Cellular -> AAC_128
            NetworkType.Other, NetworkType.None -> null
        }
    }

    private companion object {
        const val AAC_128 = "aac_128"
    }
}
