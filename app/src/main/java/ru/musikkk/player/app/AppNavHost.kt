package ru.musikkk.player.app

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ru.musikkk.player.feature.auth.LoginScreen
import ru.musikkk.player.feature.auth.RegisterScreen
import ru.musikkk.player.feature.auth.VerifyEmailScreen
import ru.musikkk.player.feature.library.LibraryScreen
import ru.musikkk.player.feature.player.PlayerScreen
import ru.musikkk.player.feature.release.ReleaseDetailScreen
import ru.musikkk.player.feature.release.ReleaseDetailViewModel
import ru.musikkk.player.feature.search.SearchScreen
import ru.musikkk.player.feature.settings.SettingsScreen

object Routes {
    const val Login = "login"
    const val Register = "register"
    const val VerifyEmail = "verify_email"
    const val Library = "library"
    const val Player = "player"
    const val Settings = "settings"
    const val Search = "search"

    private const val RELEASE = "release"

    /** Шаблон маршрута для NavHost: `release/{releaseId}`. */
    const val ReleasePattern = "$RELEASE/{${ReleaseDetailViewModel.ARG_RELEASE_ID}}"

    /**
     * id релиза собирается из `artist|section|name` и может содержать
     * символы, неподходящие для path-сегмента URI (`/`, `?`, `#`, …),
     * поэтому при построении маршрута энкодим, а в ViewModel декодим.
     */
    fun release(releaseId: String): String = "$RELEASE/${Uri.encode(releaseId)}"
}

/**
 * Корневой навигационный граф. Стартовый маршрут выбирается из
 * [AppViewModel.startDestination] — пока он `null`, показываем индикатор
 * загрузки, чтобы не было «вспышки» экрана входа у уже залогиненного
 * пользователя или пользователя на середине верификации.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val start by appViewModel.startDestination.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (start == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        NavHost(
            navController = navController,
            startDestination = start!!,
        ) {
            composable(Routes.Login) {
                LoginScreen(
                    onAuthenticated = {
                        navController.navigate(Routes.Library) {
                            popUpTo(Routes.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onRegisterClick = {
                        navController.navigate(Routes.Register) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.Register) {
                RegisterScreen(
                    onAuthenticated = {
                        navController.navigate(Routes.Library) {
                            popUpTo(Routes.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNeedsEmailVerification = {
                        navController.navigate(Routes.VerifyEmail) {
                            popUpTo(Routes.Login) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onBackToLogin = {
                        if (!navController.popBackStack(Routes.Login, inclusive = false)) {
                            navController.navigate(Routes.Login) {
                                popUpTo(Routes.Register) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }

            composable(Routes.VerifyEmail) {
                VerifyEmailScreen(
                    onVerified = {
                        navController.navigate(Routes.Library) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoToLogin = {
                        navController.navigate(Routes.Login) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onUseDifferentEmail = {
                        navController.navigate(Routes.Register) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.Library) {
                LibraryScreen(
                    onSignedOut = {
                        navController.navigate(Routes.Login) {
                            popUpTo(Routes.Library) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onReleaseClick = { release ->
                        navController.navigate(Routes.release(release.id))
                    },
                    onOpenSettings = {
                        navController.navigate(Routes.Settings) {
                            launchSingleTop = true
                        }
                    },
                    onOpenSearch = {
                        navController.navigate(Routes.Search) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.Settings) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.Search) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onReleaseClick = { release ->
                        navController.navigate(Routes.release(release.id))
                    },
                    onTrackClick = { track ->
                        // Открываем релиз трека — пользователь сможет
                        // запустить воспроизведение оттуда.
                        navController.navigate(Routes.release(track.releaseId))
                    },
                )
            }

            composable(
                route = Routes.ReleasePattern,
                arguments = listOf(
                    navArgument(ReleaseDetailViewModel.ARG_RELEASE_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                ReleaseDetailScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.Player) {
                PlayerScreen(
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}
