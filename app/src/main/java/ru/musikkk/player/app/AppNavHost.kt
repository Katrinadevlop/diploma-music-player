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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.musikkk.player.feature.auth.LoginScreen
import ru.musikkk.player.feature.library.LibraryScreen
import ru.musikkk.player.feature.release.ReleaseDetailScreen
import ru.musikkk.player.feature.release.ReleaseDetailViewModel

object Routes {
    const val Login = "login"
    const val Library = "library"

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
 * пользователя.
 */
@Composable
fun AppNavHost(
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

        val controller = rememberNavController()
        NavHost(
            navController = controller,
            startDestination = start!!,
        ) {
            composable(Routes.Login) {
                LoginScreen(
                    onAuthenticated = {
                        controller.navigate(Routes.Library) {
                            popUpTo(Routes.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.Library) {
                LibraryScreen(
                    onSignedOut = {
                        controller.navigate(Routes.Login) {
                            popUpTo(Routes.Library) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onReleaseClick = { release ->
                        controller.navigate(Routes.release(release.id))
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
                    onBack = { controller.popBackStack() },
                )
            }
        }
    }
}
