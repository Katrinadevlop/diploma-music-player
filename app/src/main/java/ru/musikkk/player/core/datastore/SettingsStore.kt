package ru.musikkk.player.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.musikkk.player.domain.settings.AppLanguage
import ru.musikkk.player.domain.settings.LibraryFilters
import ru.musikkk.player.domain.settings.SectionFilter
import ru.musikkk.player.domain.settings.StreamQuality
import ru.musikkk.player.domain.settings.ThemeMode
import ru.musikkk.player.domain.settings.UserSettings

/**
 * DataStore для пользовательских настроек. Отдельный файл от
 * [authDataStore] — настройки переживают logout/login, а токен нет.
 */
private val Context.settingsDataStore by preferencesDataStore(name = "musikkk_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val AppLanguage = stringPreferencesKey("app_language")
        val StreamQuality = stringPreferencesKey("stream_quality")
        val SectionFilter = stringPreferencesKey("library_section_filter")
        val OnlyDownloaded = booleanPreferencesKey("library_only_downloaded")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data
        .map { prefs ->
            UserSettings(
                themeMode = decodeEnum(prefs[Keys.ThemeMode], ThemeMode.entries, ThemeMode.System),
                appLanguage = decodeEnum(prefs[Keys.AppLanguage], AppLanguage.entries, AppLanguage.System),
                streamQuality = decodeEnum(prefs[Keys.StreamQuality], StreamQuality.entries, StreamQuality.Auto),
                libraryFilters = LibraryFilters(
                    sectionFilter = decodeEnum(prefs[Keys.SectionFilter], SectionFilter.entries, SectionFilter.All),
                    showOnlyDownloaded = prefs[Keys.OnlyDownloaded] ?: false,
                ),
            )
        }
        .distinctUntilChanged()

    suspend fun setThemeMode(value: ThemeMode) = update { it[Keys.ThemeMode] = value.name }
    suspend fun setAppLanguage(value: AppLanguage) = update { it[Keys.AppLanguage] = value.name }
    suspend fun setStreamQuality(value: StreamQuality) = update { it[Keys.StreamQuality] = value.name }
    suspend fun setSectionFilter(value: SectionFilter) = update { it[Keys.SectionFilter] = value.name }
    suspend fun setOnlyDownloaded(value: Boolean) = update { it[Keys.OnlyDownloaded] = value }

    private suspend inline fun update(
        crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
    ) {
        context.settingsDataStore.edit { prefs -> block(prefs) }
    }

    private fun <E : Enum<E>> decodeEnum(raw: String?, entries: List<E>, fallback: E): E {
        if (raw.isNullOrBlank()) return fallback
        return entries.firstOrNull { it.name == raw } ?: fallback
    }
}
