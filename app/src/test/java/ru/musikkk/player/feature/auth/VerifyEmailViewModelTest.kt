package ru.musikkk.player.feature.auth

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.musikkk.player.R
import ru.musikkk.player.core.datastore.PendingVerification
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.data.auth.PendingVerificationCreds
import ru.musikkk.player.domain.auth.AuthError
import ru.musikkk.player.domain.auth.AuthSession
import ru.musikkk.player.domain.auth.AuthUser

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEmailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var creds: PendingVerificationCreds

    private val pendingFlow = MutableStateFlow<PendingVerification?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = mockk(relaxed = true)
        creds = mockk(relaxed = true)
        coEvery { authRepository.pendingVerificationFlow } returns pendingFlow
        coEvery { authRepository.tokenFlow } returns MutableStateFlow<String?>(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `пустой pending показывает loadingInitial false с пустым email`() = runTest {
        val viewModel = VerifyEmailViewModel(authRepository, creds)
        viewModel.state.test {
            // первое значение — initialValue из stateIn
            skipItems(1)
            advanceUntilIdle()
            val s = expectMostRecentItem()
            assertEquals("", s.email)
            assertEquals(false, s.isLoadingInitial)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `подтверждение с in-memory creds вызывает login и эмитит Verified`() = runTest {
        val now = System.currentTimeMillis()
        pendingFlow.value = PendingVerification(
            email = "a@b.com",
            username = "alice",
            createdAtMs = now,
            resendBlockedUntilMs = now - 1_000,
        )
        every { creds.read() } returns PendingVerificationCreds.Credentials("alice", "p")
        coEvery { authRepository.login("alice", "p") } returns AuthSession(
            token = "t", expiresAtMs = 1L, user = AuthUser("p", "alice"),
        )

        val viewModel = VerifyEmailViewModel(authRepository, creds)

        viewModel.events.test {
            advanceUntilIdle()
            viewModel.onConfirmedClick()
            advanceUntilIdle()
            assertEquals(VerifyEmailEvent.Verified, awaitItem())
        }
    }

    @Test
    fun `EmailNotVerified показывается как info а не как error`() = runTest {
        val now = System.currentTimeMillis()
        pendingFlow.value = PendingVerification(
            email = "a@b.com",
            username = "alice",
            createdAtMs = now,
            resendBlockedUntilMs = now - 1_000,
        )
        every { creds.read() } returns PendingVerificationCreds.Credentials("alice", "p")
        coEvery { authRepository.login(any(), any()) } throws AuthError.EmailNotVerified

        val viewModel = VerifyEmailViewModel(authRepository, creds)
        advanceUntilIdle()
        viewModel.onConfirmedClick()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertEquals(R.string.verify_email_not_verified_yet, s.infoRes)
        assertNull(s.errorRes)
    }

    @Test
    fun `confirmed click без in-memory creds эмитит GoToLogin`() = runTest {
        val now = System.currentTimeMillis()
        pendingFlow.value = PendingVerification(
            email = "a@b.com",
            username = "alice",
            createdAtMs = now,
            resendBlockedUntilMs = now - 1_000,
        )
        every { creds.read() } returns null

        val viewModel = VerifyEmailViewModel(authRepository, creds)
        viewModel.events.test {
            advanceUntilIdle()
            viewModel.onConfirmedClick()
            advanceUntilIdle()
            assertEquals(VerifyEmailEvent.GoToLogin, awaitItem())
        }

        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `resend во время cooldown не дёргает сервер`() = runTest {
        val now = System.currentTimeMillis()
        pendingFlow.value = PendingVerification(
            email = "a@b.com",
            username = "alice",
            createdAtMs = now,
            resendBlockedUntilMs = now + 30_000,  // cooldown активен
        )
        every { creds.read() } returns null

        val viewModel = VerifyEmailViewModel(authRepository, creds)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.resendCooldownSeconds > 0)

        viewModel.onResendClick()
        advanceUntilIdle()

        coVerify(exactly = 0) { authRepository.resendVerificationEmail(any()) }
    }

    @Test
    fun `успешный resend показывает infoRes`() = runTest {
        val now = System.currentTimeMillis()
        pendingFlow.value = PendingVerification(
            email = "a@b.com",
            username = "alice",
            createdAtMs = now,
            resendBlockedUntilMs = now - 1_000,
        )
        every { creds.read() } returns null
        coEvery { authRepository.resendVerificationEmail("a@b.com") } returns 60

        val viewModel = VerifyEmailViewModel(authRepository, creds)
        advanceUntilIdle()
        viewModel.onResendClick()
        advanceUntilIdle()

        assertEquals(R.string.verify_email_resend_sent, viewModel.state.value.infoRes)
        assertNull(viewModel.state.value.errorRes)
    }

    @Test
    fun `useDifferentEmail чистит pending и эмитит событие`() = runTest {
        val now = System.currentTimeMillis()
        pendingFlow.value = PendingVerification("a@b.com", "alice", now, now - 1_000)
        coEvery { authRepository.clearPendingVerification() } just Runs

        val viewModel = VerifyEmailViewModel(authRepository, creds)
        viewModel.events.test {
            advanceUntilIdle()
            viewModel.onUseDifferentEmailClick()
            advanceUntilIdle()
            assertEquals(VerifyEmailEvent.UseDifferentEmail, awaitItem())
        }
        coVerify { authRepository.clearPendingVerification() }
    }
}
