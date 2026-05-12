package ru.musikkk.player.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.musikkk.player.domain.settings.StreamQuality

class NetworkQualityResolverTest {

    @Test
    fun `Original всегда null независимо от сети`() {
        for (type in NetworkType.entries) {
            val resolver = NetworkQualityResolver(provider(type))
            assertNull(resolver.preferredVariant(StreamQuality.Original))
        }
    }

    @Test
    fun `Aac128 всегда aac_128 независимо от сети`() {
        for (type in NetworkType.entries) {
            val resolver = NetworkQualityResolver(provider(type))
            assertEquals("aac_128", resolver.preferredVariant(StreamQuality.Aac128))
        }
    }

    @Test
    fun `Auto на WiFi и Ethernet - оригинал`() {
        assertNull(NetworkQualityResolver(provider(NetworkType.WiFi)).preferredVariant(StreamQuality.Auto))
        assertNull(NetworkQualityResolver(provider(NetworkType.Ethernet)).preferredVariant(StreamQuality.Auto))
    }

    @Test
    fun `Auto на мобильной сети - aac_128`() {
        assertEquals(
            "aac_128",
            NetworkQualityResolver(provider(NetworkType.Cellular)).preferredVariant(StreamQuality.Auto),
        )
    }

    @Test
    fun `Auto без сети или непонятный транспорт - оригинал`() {
        assertNull(NetworkQualityResolver(provider(NetworkType.None)).preferredVariant(StreamQuality.Auto))
        assertNull(NetworkQualityResolver(provider(NetworkType.Other)).preferredVariant(StreamQuality.Auto))
    }

    private fun provider(type: NetworkType): NetworkTypeProvider = object : NetworkTypeProvider {
        override fun current(): NetworkType = type
    }
}
