package ru.musikkk.player.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * Корневой навигационный граф. Реальный auth-gating (читать [TokenStore]
 * и выбирать стартовый экран) приедет вместе с auth-фичей. Сейчас всегда
 * стартуем с Login, чтобы каркас компилировался end-to-end.
 */
@Composable
fun AppNavHost() {
    val controller = rememberNavController()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController = controller,
            startDestination = Routes.Login,
        ) {
            composable(Routes.Login) {
                LoginScreen(
                    onAuthenticated = {
                        controller.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.Home) {
                HomeScreen()
            }
        }
    }
}
