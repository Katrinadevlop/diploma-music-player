package ru.musikkk.player.feature.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.musikkk.player.R
import ru.musikkk.player.ui.components.PlaybackAwareBackdrop
import ru.musikkk.player.ui.components.SectionCover
import ru.musikkk.player.ui.theme.MusikkkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    onBack: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenTop: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        PlaybackAwareBackdrop()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.hub_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.release_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
            containerColor = Color.Transparent,
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(MusikkkSpacing.s4),
                verticalArrangement = Arrangement.spacedBy(MusikkkSpacing.s3),
            ) {
                item {
                    HubTile(
                        icon = Icons.Filled.Favorite,
                        title = stringResource(id = R.string.hub_liked),
                        subtitle = stringResource(id = R.string.hub_liked_subtitle),
                        onClick = onOpenLiked,
                    )
                }
                item {
                    HubTile(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        title = stringResource(id = R.string.hub_playlists),
                        subtitle = stringResource(id = R.string.hub_playlists_subtitle),
                        onClick = onOpenPlaylists,
                    )
                }
                item {
                    HubTile(
                        icon = Icons.Filled.History,
                        title = stringResource(id = R.string.hub_recent),
                        subtitle = stringResource(id = R.string.hub_recent_subtitle),
                        onClick = onOpenRecent,
                    )
                }
                item {
                    HubTile(
                        icon = Icons.Filled.Whatshot,
                        title = stringResource(id = R.string.hub_top),
                        subtitle = stringResource(id = R.string.hub_top_subtitle),
                        onClick = onOpenTop,
                    )
                }
            }
        }
    }
}

@Composable
private fun HubTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(MusikkkSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionCover(
            icon = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.size(MusikkkSpacing.s4))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
