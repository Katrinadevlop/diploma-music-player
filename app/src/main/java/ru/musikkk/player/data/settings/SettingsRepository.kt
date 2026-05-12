package ru.musikkk.player.data.settings

import kotlinx.coroutines.flow.Flow
import ru.musikkk.player.domain.settings.AppLanguage
import ru.musikkk.player.domain.settings.SectionFilter
import ru.musikkk.player.domain.settings.StreamQuality
import ru.musikkk.player.domain.settings.ThemeMode
import ru.musikkk.player.domain.settings.UserSettings

interface SettingsRepository {
    val settingsFlow: Flow<UserSettings>

    suspend fun setThemeMode(value: ThemeMode)
    suspend fun setAppLanguage(value: AppLanguage)
    suspend fun setStreamQuality(value: StreamQuality)
    suspend fun setSectionFilter(value: SectionFilter)
    suspend fun setOnlyDownloaded(value: Boolean)
}
