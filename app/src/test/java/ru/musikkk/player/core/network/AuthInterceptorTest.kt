package ru.musikkk.player.core.network

import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.musikkk.player.core.datastore.TokenStore

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: TokenStore

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokenStore = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `adds Authorization header when token is stored`() {
        every { tokenStore.cachedToken() } returns "session-token-abc"
        server.enqueue(MockResponse().setResponseCode(204))

        client().newCall(Request.Builder().url(server.url("/api/me")).build()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer session-token-abc", recorded.getHeader("Authorization"))
    }

    @Test
    fun `does not add Authorization header when no token is stored`() {
        every { tokenStore.cachedToken() } returns null
        server.enqueue(MockResponse().setResponseCode(204))

        client().newCall(Request.Builder().url(server.url("/api/auth/token")).build()).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `does not overwrite an Authorization header set by the caller`() {
        every { tokenStore.cachedToken() } returns "session-token-abc"
        server.enqueue(MockResponse().setResponseCode(204))

        client().newCall(
            Request.Builder()
                .url(server.url("/api/me"))
                .header("Authorization", "Bearer caller-supplied")
                .build()
        ).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer caller-supplied", recorded.getHeader("Authorization"))
    }

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .build()
}
