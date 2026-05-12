package ru.musikkk.player.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.musikkk.player.R
import ru.musikkk.player.domain.settings.AppLanguage
import ru.musikkk.player.domain.settings.SectionFilter
import ru.musikkk.player.domain.settings.StreamQuality
import ru.musikkk.player.domain.settings.ThemeMode
import ru.musikkk.player.ui.theme.MusikkkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickerState = remember { mutableStateOf<PickerKind?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.release_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = MusikkkSpacing.s3),
        ) {
            item { SectionHeader(stringResource(id = R.string.settings_section_appearance)) }
            item {
                SettingRow(
                    title = stringResource(id = R.string.settings_theme),
                    value = themeLabel(state.themeMode),
                    onClick = { pickerState.value = PickerKind.Theme },
                )
            }
            item {
                SettingRow(
                    title = stringResource(id = R.string.settings_language),
                    value = languageLabel(state.appLanguage),
                    onClick = { pickerState.value = PickerKind.Language },
                )
            }

            item { Divider() }
            item { SectionHeader(stringResource(id = R.string.settings_section_playback)) }
            item {
                SettingRow(
                    title = stringResource(id = R.string.settings_stream_quality),
                    value = streamQualityLabel(state.streamQuality),
                    onClick = { pickerState.value = PickerKind.StreamQuality },
                )
            }

            item { Divider() }
            item { SectionHeader(stringResource(id = R.string.settings_section_library_filters)) }
            item {
                SettingRow(
                    title = stringResource(id = R.string.settings_section_filter),
                    value = sectionFilterLabel(state.libraryFilters.sectionFilter),
                    onClick = { pickerState.value = PickerKind.SectionFilter },
                )
            }
            item {
                SwitchRow(
                    title = stringResource(id = R.string.settings_only_downloaded),
                    checked = state.libraryFilters.showOnlyDownloaded,
                    onCheckedChange = viewModel::setOnlyDownloaded,
                )
            }

            item { Divider() }
            item { SectionHeader(stringResource(id = R.string.settings_section_account)) }
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                SettingRow(
                    title = stringResource(id = R.string.settings_manage_subscription),
                    value = stringResource(id = R.string.settings_open_in_browser),
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            androidx.core.net.Uri.parse("https://musikkk.ru/billing"),
                        )
                        runCatching { context.startActivity(intent) }
                    },
                )
            }
        }
    }

    when (val kind = pickerState.value) {
        null -> Unit
        PickerKind.Theme -> ChoicePicker(
            title = stringResource(id = R.string.settings_theme),
            options = ThemeMode.entries,
            selected = state.themeMode,
            labelFor = { themeLabel(it) },
            onSelect = {
                viewModel.setThemeMode(it)
                pickerState.value = null
            },
            onDismiss = { pickerState.value = null },
        )
        PickerKind.Language -> ChoicePicker(
            title = stringResource(id = R.string.settings_language),
            options = AppLanguage.entries,
            selected = state.appLanguage,
            labelFor = { languageLabel(it) },
            onSelect = {
                viewModel.setAppLanguage(it)
                pickerState.value = null
            },
            onDismiss = { pickerState.value = null },
        )
        PickerKind.StreamQuality -> ChoicePicker(
            title = stringResource(id = R.string.settings_stream_quality),
            options = StreamQuality.entries,
            selected = state.streamQuality,
            labelFor = { streamQualityLabel(it) },
            onSelect = {
                viewModel.setStreamQuality(it)
                pickerState.value = null
            },
            onDismiss = { pickerState.value = null },
        )
        PickerKind.SectionFilter -> ChoicePicker(
            title = stringResource(id = R.string.settings_section_filter),
            options = SectionFilter.entries,
            selected = state.libraryFilters.sectionFilter,
            labelFor = { sectionFilterLabel(it) },
            onSelect = {
                viewModel.setSectionFilter(it)
                pickerState.value = null
            },
            onDismiss = { pickerState.value = null },
        )
    }
}

private enum class PickerKind { Theme, Language, StreamQuality, SectionFilter }

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = MusikkkSpacing.s5,
            vertical = MusikkkSpacing.s3,
        ),
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = MusikkkSpacing.s2),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoicePicker(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MusikkkSpacing.s4),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MusikkkSpacing.s3),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(
                        horizontal = MusikkkSpacing.s5,
                        vertical = MusikkkSpacing.s3,
                    ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                ) {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option == selected,
                                    onClick = { onSelect(option) },
                                    role = androidx.compose.ui.semantics.Role.RadioButton,
                                )
                                .padding(
                                    horizontal = MusikkkSpacing.s5,
                                    vertical = MusikkkSpacing.s3,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = option == selected,
                                onClick = null,
                            )
                            Text(
                                text = labelFor(option),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = MusikkkSpacing.s3),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun themeLabel(value: ThemeMode): String = stringResource(
    id = when (value) {
        ThemeMode.System -> R.string.settings_theme_system
        ThemeMode.Light -> R.string.settings_theme_light
        ThemeMode.Dark -> R.string.settings_theme_dark
    },
)

@Composable
private fun languageLabel(value: AppLanguage): String = stringResource(
    id = when (value) {
        AppLanguage.System -> R.string.settings_language_system
        AppLanguage.Ru -> R.string.settings_language_ru
        AppLanguage.En -> R.string.settings_language_en
    },
)

@Composable
private fun streamQualityLabel(value: StreamQuality): String = stringResource(
    id = when (value) {
        StreamQuality.Auto -> R.string.settings_stream_quality_auto
        StreamQuality.Original -> R.string.settings_stream_quality_original
        StreamQuality.Aac128 -> R.string.settings_stream_quality_aac128
    },
)

@Composable
private fun sectionFilterLabel(value: SectionFilter): String = stringResource(
    id = when (value) {
        SectionFilter.All -> R.string.settings_section_filter_all
        SectionFilter.Albums -> R.string.library_section_albums
        SectionFilter.Eps -> R.string.library_section_eps
        SectionFilter.Singles -> R.string.library_section_singles
        SectionFilter.Collabs -> R.string.library_section_collabs
    },
)
