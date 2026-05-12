package ru.musikkk.player.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton
import ru.musikkk.player.domain.settings.AppLanguage

/**
 * Применяет выбранный пользователем язык к приложению через
 * `AppCompatDelegate.setApplicationLocales`. AppCompat сам:
 *   - сохраняет локаль на диск (`androidx.appcompat.app.AppLocalesStorage`);
 *   - пересоздаёт текущую Activity, чтобы строки пересчитались;
 *   - на API 33+ делегирует системному `LocaleManager`.
 *
 * `AppLanguage.System` — пустой `LocaleListCompat`, что означает «следовать
 * локали устройства».
 */
@Singleton
class LocaleApplier @Inject constructor() {

    fun apply(language: AppLanguage) {
        val tag = language.bcp47
        val locales = if (tag.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }

        // Идемпотентно: если уже стоит то же значение, AppCompat не дёргает recreate.
        val current = AppCompatDelegate.getApplicationLocales()
        if (current == locales) return

        AppCompatDelegate.setApplicationLocales(locales)
    }
}
