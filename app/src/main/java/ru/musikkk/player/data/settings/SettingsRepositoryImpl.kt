package ru.musikkk.player.data.settings

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import ru.musikkk.player.core.datastore.SettingsStore
import ru.musikkk.player.domain.settings.AppLanguage
import ru.musikkk.player.domain.settings.SectionFilter
import ru.musikkk.player.domain.settings.StreamQuality
import ru.musikkk.player.domain.settings.ThemeMode
import ru.musikkk.player.domain.settings.UserSettings

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsStore,
) : SettingsRepository {

    override val settingsFlow: Flow<UserSettings> = store.settingsFlow

    override suspend fun setThemeMode(value: ThemeMode) = store.setThemeMode(value)
    override suspend fun setAppLanguage(value: AppLanguage) = store.setAppLanguage(value)
    override suspend fun setStreamQuality(value: StreamQuality) = store.setStreamQuality(value)
    override suspend fun setSectionFilter(value: SectionFilter) = store.setSectionFilter(value)
    override suspend fun setOnlyDownloaded(value: Boolean) = store.setOnlyDownloaded(value)
}
