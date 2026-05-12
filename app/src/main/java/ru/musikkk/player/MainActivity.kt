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
import dagger.hilt.android.AndroidEntryPoint
import ru.musikkk.player.app.MainScaffold
import ru.musikkk.player.ui.theme.MusikkkTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Результат не критичен: если пользователь отказал, плеер всё равно
    // воспроизводит трек — просто без системной нотификации управления.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            MusikkkTheme {
                MainScaffold()
            }
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
