package ru.musikkk.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.musikkk.player.data.settings.SettingsRepository
import ru.musikkk.player.domain.settings.AppLanguage
import ru.musikkk.player.domain.settings.SectionFilter
import ru.musikkk.player.domain.settings.StreamQuality
import ru.musikkk.player.domain.settings.ThemeMode
import ru.musikkk.player.domain.settings.UserSettings

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = UserSettings(),
        )

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(value)
    }

    fun setAppLanguage(value: AppLanguage) = viewModelScope.launch {
        settingsRepository.setAppLanguage(value)
    }

    fun setStreamQuality(value: StreamQuality) = viewModelScope.launch {
        settingsRepository.setStreamQuality(value)
    }

    fun setSectionFilter(value: SectionFilter) = viewModelScope.launch {
        settingsRepository.setSectionFilter(value)
    }

    fun setOnlyDownloaded(value: Boolean) = viewModelScope.launch {
        settingsRepository.setOnlyDownloaded(value)
    }
}
