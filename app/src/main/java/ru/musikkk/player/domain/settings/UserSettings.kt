package ru.musikkk.player.domain.settings

/** Режим темы оформления. `System` — следовать системной настройке. */
enum class ThemeMode { System, Light, Dark }

/** Язык интерфейса. `System` — следовать локали устройства. */
enum class AppLanguage(val bcp47: String?) {
    System(null),
    Ru("ru"),
    En("en"),
}

/**
 * Качество стрима из сервера.
 * - `Auto`  — по типу сети: Wi-Fi/Ethernet → оригинал, мобильная → `aac_128`.
 * - `Original` — всегда оригинал (может быть FLAC, большой трафик).
 * - `Aac128`  — всегда `?v=aac_128` (сервер сам fallback'нет на оригинал,
 *   если конкретный трек не имеет такого варианта).
 */
enum class StreamQuality { Auto, Original, Aac128 }

/** Фильтр по типу релиза в библиотеке. */
enum class SectionFilter {
    All, Albums, Eps, Singles, Collabs;

    /** Соответствие `Release.section.raw`. `All` — без фильтра. */
    fun matchesRaw(raw: String): Boolean = when (this) {
        All -> true
        Albums -> raw == "albums"
        Eps -> raw == "eps"
        Singles -> raw == "singles"
        Collabs -> raw == "collabs"
    }
}

data class LibraryFilters(
    val sectionFilter: SectionFilter = SectionFilter.All,
    val showOnlyDownloaded: Boolean = false,
)

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val appLanguage: AppLanguage = AppLanguage.System,
    val streamQuality: StreamQuality = StreamQuality.Auto,
    val libraryFilters: LibraryFilters = LibraryFilters(),
)
