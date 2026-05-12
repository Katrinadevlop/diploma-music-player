package ru.musikkk.player.core.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Тип активного сетевого соединения. Используется для выбора качества
 * стрима — по требованию пользователя на Wi-Fi/Ethernet берём оригинал,
 * на мобильной сети — `aac_128`, если этот вариант доступен у трека.
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
     * Возвращает имя варианта (`aac_128` / `mp3_320`) или `null` — значит
     * стримим оригинал. `availableVariants` приходят из ответа сервера
     * по конкретному треку: для lossless источников может быть и `mp3_320`,
     * для остальных — только `aac_128`.
     */
    fun preferredVariant(availableVariants: Set<String>): String? =
        when (networkTypeProvider.current()) {
            NetworkType.WiFi, NetworkType.Ethernet -> null
            NetworkType.Cellular -> when {
                AAC_128 in availableVariants -> AAC_128
                else -> null
            }
            NetworkType.Other, NetworkType.None -> null
        }

    private companion object {
        const val AAC_128 = "aac_128"
    }
}
