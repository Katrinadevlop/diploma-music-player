package ru.musikkk.player.feature.auth

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.musikkk.player.R
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.data.auth.RegisterOutcome
import ru.musikkk.player.domain.auth.AuthError
import ru.musikkk.player.domain.auth.AuthSession
import ru.musikkk.player.domain.auth.AuthUser

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = mockk(relaxed = true)
        coEvery { authRepository.tokenFlow } returns MutableStateFlow<String?>(null)
        coEvery { authRepository.pendingVerificationFlow } returns MutableStateFlow(null)
        viewModel = RegisterViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `клиентская валидация ловит короткий пароль не дёргая сеть`() = runTest {
        fillValid()
        viewModel.onPasswordChange("short")
        viewModel.onPasswordConfirmChange("short")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(R.string.register_error_password_too_short, viewModel.state.value.passwordError)
        coVerify(exactly = 0) { authRepository.register(any(), any(), any()) }
    }

    @Test
    fun `несовпадающие пароли подсвечивают второе поле`() = runTest {
        fillValid()
        viewModel.onPasswordConfirmChange("differentPass1!")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(
            R.string.register_error_password_mismatch,
            viewModel.state.value.passwordConfirmError,
        )
        coVerify(exactly = 0) { authRepository.register(any(), any(), any()) }
    }

    @Test
    fun `неверный паттерн username ловится клиентом`() = runTest {
        fillValid()
        viewModel.onUsernameChange("ab")  // < 3 chars
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(R.string.register_error_username_invalid, viewModel.state.value.usernameError)
    }

    @Test
    fun `несогласие с terms блокирует submit`() = runTest {
        fillValid()
        viewModel.onAcceptTermsChange(false)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(R.string.register_error_terms_required, viewModel.state.value.termsError)
    }

    @Test
    fun `email_taken от сервера попадает на поле email`() = runTest {
        coEvery {
            authRepository.register("alice", "Password1!", "a@b.com")
        } throws AuthError.EmailTaken

        fillValid()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(R.string.register_error_email_taken, viewModel.state.value.emailError)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `username_taken от сервера попадает на поле username`() = runTest {
        coEvery {
            authRepository.register(any(), any(), any())
        } throws AuthError.UsernameTaken

        fillValid()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(R.string.register_error_username_taken, viewModel.state.value.usernameError)
    }

    @Test
    fun `NeedsEmailVerification эмитит событие с email`() = runTest {
        coEvery {
            authRepository.register("alice", "Password1!", "a@b.com")
        } returns RegisterOutcome.NeedsEmailVerification(retryAfterSeconds = 60)

        fillValid()

        viewModel.events.test {
            viewModel.submit()
            advanceUntilIdle()

            val ev = awaitItem()
            assertEquals(RegisterEvent.NeedsEmailVerification("a@b.com"), ev)
        }
    }

    @Test
    fun `LoggedIn эмитит Registered`() = runTest {
        coEvery {
            authRepository.register(any(), any(), any())
        } returns RegisterOutcome.LoggedIn(
            AuthSession(token = "t", expiresAtMs = 1L, user = AuthUser("p", "alice")),
        )

        fillValid()

        viewModel.events.test {
            viewModel.submit()
            advanceUntilIdle()

            assertEquals(RegisterEvent.Registered, awaitItem())
        }
    }

    @Test
    fun `редактирование поля сбрасывает соответствующую ошибку`() = runTest {
        coEvery { authRepository.register(any(), any(), any()) } throws AuthError.EmailTaken
        fillValid()
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(R.string.register_error_email_taken, viewModel.state.value.emailError)

        viewModel.onEmailChange("new@b.com")
        assertNull(viewModel.state.value.emailError)
    }

    private fun fillValid() {
        viewModel.onUsernameChange("alice")
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("Password1!")
        viewModel.onPasswordConfirmChange("Password1!")
        viewModel.onAcceptTermsChange(true)
    }
}
