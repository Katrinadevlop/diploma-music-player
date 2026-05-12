package ru.musikkk.player.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.musikkk.player.data.settings.SettingsRepository
import ru.musikkk.player.domain.settings.ThemeMode

/**
 * ViewModel-«крыша» для `MainScaffold`: достаёт из настроек только то,
 * что нужно знать на уровне всей оболочки — пока это режим темы.
 * Язык применяется ниже, в `MainActivity.onCreate`, так как требует
 * пересоздания Activity и не вписывается в Compose-recomposition.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.settingsFlow
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.System,
        )
}
