package ru.musikkk.player.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.musikkk.player.R
import ru.musikkk.player.ui.theme.MusikkkSpacing

/** Заглушка экрана авторизации — реальный UI добавим следующей итерацией. */
@Composable
fun LoginScreen(
    @Suppress("UNUSED_PARAMETER") onAuthenticated: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MusikkkSpacing.s5),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(id = R.string.auth_login_title))
    }
}
