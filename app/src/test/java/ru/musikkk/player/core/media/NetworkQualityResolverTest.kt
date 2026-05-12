package ru.musikkk.player.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkQualityResolverTest {

    @Test
    fun `на WiFi всегда стримим оригинал`() {
        val resolver = NetworkQualityResolver(provider(NetworkType.WiFi))
        assertNull(resolver.preferredVariant(setOf("aac_128", "mp3_320")))
    }

    @Test
    fun `на Ethernet тоже оригинал`() {
        val resolver = NetworkQualityResolver(provider(NetworkType.Ethernet))
        assertNull(resolver.preferredVariant(setOf("aac_128")))
    }

    @Test
    fun `на мобильной сети выбирается aac_128 если доступен`() {
        val resolver = NetworkQualityResolver(provider(NetworkType.Cellular))
        assertEquals("aac_128", resolver.preferredVariant(setOf("aac_128", "mp3_320")))
    }

    @Test
    fun `на мобильной сети без aac_128 fallback на оригинал`() {
        val resolver = NetworkQualityResolver(provider(NetworkType.Cellular))
        assertNull(resolver.preferredVariant(setOf("mp3_320")))
        assertNull(resolver.preferredVariant(emptySet()))
    }

    @Test
    fun `неизвестный транспорт - оригинал`() {
        val resolver = NetworkQualityResolver(provider(NetworkType.Other))
        assertNull(resolver.preferredVariant(setOf("aac_128")))
    }

    @Test
    fun `без сети - оригинал`() {
        val resolver = NetworkQualityResolver(provider(NetworkType.None))
        assertNull(resolver.preferredVariant(setOf("aac_128")))
    }

    private fun provider(type: NetworkType): NetworkTypeProvider = object : NetworkTypeProvider {
        override fun current(): NetworkType = type
    }
}
