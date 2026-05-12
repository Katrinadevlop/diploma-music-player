package ru.musikkk.player.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.musikkk.player.feature.player.MiniPlayer
import ru.musikkk.player.feature.player.PlaybackViewModel
import ru.musikkk.player.ui.theme.MusikkkTheme

/**
 * Корневой контейнер UI: оборачивает граф навигации в актуальную тему,
 * а снизу клеит `MiniPlayer`. На полноэкранном маршруте плеера он прячется.
 * Тема и язык применяются глобально из пользовательских настроек — тема
 * через `MusikkkTheme`, локаль — через `LocaleApplier` в `MainActivity`.
 */
@Composable
fun MainScaffold(rootViewModel: RootViewModel = hiltViewModel()) {
    val themeMode by rootViewModel.themeMode.collectAsStateWithLifecycle()

    MusikkkTheme(themeMode = themeMode) {
        AppShell()
    }
}

@Composable
private fun AppShell() {
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val playbackState by playbackViewModel.state.collectAsStateWithLifecycle()

    val controller = rememberNavController()
    val backStackEntry by controller.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val miniPlayerVisible = playbackState.isActive && currentRoute != Routes.Player

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            AppNavHost(navController = controller)
        }

        if (miniPlayerVisible) {
            MiniPlayer(
                state = playbackState,
                onTogglePlay = playbackViewModel::togglePlayPause,
                onSkipNext = playbackViewModel::skipNext,
                onExpand = {
                    controller.navigate(Routes.Player) {
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
