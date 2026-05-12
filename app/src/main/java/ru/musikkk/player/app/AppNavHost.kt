package ru.musikkk.player.app

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.musikkk.player.feature.auth.LoginScreen
import ru.musikkk.player.feature.home.HomeScreen

object Routes {
    const val Login = "login"
    const val Home = "home"
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
                        controller.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.Home) {
                HomeScreen(
                    onSignedOut = {
                        controller.navigate(Routes.Login) {
                            popUpTo(Routes.Home) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
