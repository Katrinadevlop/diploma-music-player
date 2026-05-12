package ru.musikkk.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.musikkk.player.app.MainScaffold
import ru.musikkk.player.core.locale.LocaleApplier
import ru.musikkk.player.data.settings.SettingsRepository

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var localeApplier: LocaleApplier

    // Результат не критичен: если пользователь отказал, плеер всё равно
    // воспроизводит трек — просто без системной нотификации управления.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyInitialLocale()
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        observeLanguageSetting()
        setContent {
            MainScaffold()
        }
    }

    /**
     * Снимаем текущее значение синхронно ещё до `setContent`, чтобы строки
     * сразу пришли на нужном языке (без вспышки английского). `runBlocking`
     * здесь оправдан: это `Activity.onCreate`, одно короткое чтение из
     * DataStore до показа UI. Внутри `LocaleApplier` идемпотентно
     * сравнивает с текущим значением AppCompat — лишнего `recreate()` не
     * будет.
     */
    private fun applyInitialLocale() {
        val language = runBlocking { settingsRepository.settingsFlow.first().appLanguage }
        localeApplier.apply(language)
    }

    private fun observeLanguageSetting() {
        lifecycleScope.launch {
            settingsRepository.settingsFlow
                .map { it.appLanguage }
                .distinctUntilChanged()
                .collect { localeApplier.apply(it) }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        // POST_NOTIFICATIONS — runtime-разрешение с Android 13. На более
        // ранних версиях оно гранится из манифеста автоматически.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
