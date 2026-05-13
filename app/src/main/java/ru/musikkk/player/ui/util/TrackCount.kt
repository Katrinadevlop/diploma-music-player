package ru.musikkk.player.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.musikkk.player.R

/**
 * Локализованная строка количества треков. Использует CLDR-плюрализацию
 * (`R.plurals.library_track_count`), корректно работает для английского
 * (one/other) и русского (one/few/many/other).
 *
 * Спецслучай нуля — отдельная строка `R.string.library_track_count_empty`,
 * т.к. Android Resources не выбирает `quantity="zero"` автоматически
 * (CLDR-правила английского/русского такой группы не имеют).
 */
@Composable
fun tracksCountString(count: Int): String =
    if (count == 0) {
        stringResource(id = R.string.library_track_count_empty)
    } else {
        pluralStringResource(id = R.plurals.library_track_count, count = count, count)
    }
