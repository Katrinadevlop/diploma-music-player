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
import org.junit.Before
import org.junit.Test
import ru.musikkk.player.R
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.domain.auth.AuthError
import ru.musikkk.player.domain.auth.AuthSession
import ru.musikkk.player.domain.auth.AuthUser

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        // tokenFlow доступ к нему может быть из конструктора через delegate —
        // подменяем безопасным пустым потоком.
        coEvery { authRepository.tokenFlow } returns MutableStateFlow<String?>(null)
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit с пустыми полями не дёргает репозиторий и показывает ошибку`() = runTest {
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(R.string.auth_error_empty_fields, viewModel.state.value.errorRes)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `успешный submit гасит загрузку и эмитит AuthSucceeded`() = runTest {
        coEvery {
            authRepository.login("alice", "hunter2")
        } returns AuthSession(
            token = "t",
            expiresAtMs = 1L,
            user = AuthUser(publicId = "p", username = "alice"),
        )

        viewModel.onUsernameChange("alice")
        viewModel.onPasswordChange("hunter2")

        viewModel.events.test {
            viewModel.submit()
            advanceUntilIdle()

            assertEquals(LoginEvent.AuthSucceeded, awaitItem())
        }

        val final = viewModel.state.value
        assertFalse(final.isSubmitting)
        assertEquals("", final.password)
        assertEquals(null, final.errorRes)
    }

    @Test
    fun `InvalidCredentials мапится в ошибку поля и снимает submitting`() = runTest {
        coEvery { authRepository.login(any(), any()) } throws AuthError.InvalidCredentials

        viewModel.onUsernameChange("alice")
        viewModel.onPasswordChange("wrong")
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(R.string.auth_error_invalid_credentials, state.errorRes)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `сетевая ошибка мапится в auth_error_network`() = runTest {
        coEvery { authRepository.login(any(), any()) } throws AuthError.Network(IllegalStateException("boom"))

        viewModel.onUsernameChange("alice")
        viewModel.onPasswordChange("hunter2")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(R.string.auth_error_network, viewModel.state.value.errorRes)
    }

    @Test
    fun `редактирование поля сбрасывает ошибку`() = runTest {
        coEvery { authRepository.login(any(), any()) } throws AuthError.InvalidCredentials
        viewModel.onUsernameChange("alice")
        viewModel.onPasswordChange("wrong")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(R.string.auth_error_invalid_credentials, viewModel.state.value.errorRes)

        viewModel.onPasswordChange("right")
        assertEquals(null, viewModel.state.value.errorRes)
    }
}
