package ru.musikkk.player.feature.uploads

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.musikkk.player.R
import ru.musikkk.player.domain.upload.UploadInfo
import ru.musikkk.player.domain.upload.UploadStatus
import ru.musikkk.player.ui.theme.MusikkkSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadsScreen(
    onBack: () -> Unit,
    viewModel: UploadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        viewModel.enqueue(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.uploads_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.release_back),
                        )
                    }
                },
                actions = {
                    if (state.uploads.any { it.status == UploadStatus.Completed }) {
                        TextButton(onClick = viewModel::clearCompleted) {
                            Text(stringResource(id = R.string.uploads_clear_completed))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { pickerLauncher.launch(arrayOf("audio/*")) },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.uploads_pick_files),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.uploads.isEmpty()) {
                EmptyState(
                    onPickFiles = { pickerLauncher.launch(arrayOf("audio/*")) },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = MusikkkSpacing.s3),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(state.uploads, key = { it.id }) { upload ->
                        UploadRow(
                            upload = upload,
                            onCancel = { viewModel.cancel(upload.id) },
                            onRetry = { viewModel.retry(upload.id) },
                            onRemove = { viewModel.remove(upload.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onPickFiles: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MusikkkSpacing.s5),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.uploads_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(MusikkkSpacing.s2))
        Text(
            text = stringResource(id = R.string.uploads_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MusikkkSpacing.s4))
        androidx.compose.material3.OutlinedButton(onClick = onPickFiles) {
            Text(stringResource(id = R.string.uploads_pick_files))
        }
    }
}

@Composable
private fun UploadRow(
    upload: UploadInfo,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = upload.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitleFor(upload),
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor(upload.status),
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(MusikkkSpacing.s2))
            TrailingAction(
                upload = upload,
                onCancel = onCancel,
                onRetry = onRetry,
                onRemove = onRemove,
            )
        }

        when (upload.status) {
            UploadStatus.Queued, UploadStatus.Running -> {
                Spacer(Modifier.height(MusikkkSpacing.s2))
                LinearProgressIndicator(
                    progress = { upload.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            UploadStatus.Completed, UploadStatus.Failed -> Unit
        }
    }
}

@Composable
private fun TrailingAction(
    upload: UploadInfo,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    when (upload.status) {
        UploadStatus.Queued, UploadStatus.Running -> IconButton(
            onClick = onCancel,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(id = R.string.uploads_action_cancel),
            )
        }
        UploadStatus.Failed -> Row {
            IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(id = R.string.uploads_action_retry),
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(id = R.string.uploads_action_remove),
                )
            }
        }
        UploadStatus.Completed -> IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(id = R.string.uploads_action_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun subtitleFor(upload: UploadInfo): String {
    return when (upload.status) {
        UploadStatus.Queued -> stringResource(id = R.string.uploads_status_queued)
        UploadStatus.Running -> {
            val percent = (upload.progress * 100).toInt()
            stringResource(id = R.string.uploads_status_running, percent)
        }
        UploadStatus.Completed -> stringResource(id = R.string.uploads_status_completed)
        UploadStatus.Failed -> {
            val code = upload.errorCode.orEmpty()
            val label = errorLabel(code)
            stringResource(id = R.string.uploads_status_failed, label)
        }
    }
}

@Composable
private fun errorLabel(code: String): String = when (code) {
    "rate_limited" -> stringResource(id = R.string.auth_error_rate_limited)
    "file_too_large" -> stringResource(id = R.string.uploads_error_too_large)
    "unsupported_format" -> stringResource(id = R.string.uploads_error_unsupported)
    "storage_limit_exceeded" -> stringResource(id = R.string.uploads_error_quota)
    "max_flac_files_exceeded" -> stringResource(id = R.string.uploads_error_flac_quota)
    "io_error", "cannot_open_uri" -> stringResource(id = R.string.auth_error_network)
    "cancelled" -> stringResource(id = R.string.uploads_error_cancelled)
    else -> code.ifBlank { stringResource(id = R.string.auth_error_unknown) }
}

@Composable
private fun subtitleColor(status: UploadStatus) = when (status) {
    UploadStatus.Failed -> MaterialTheme.colorScheme.error
    UploadStatus.Completed -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
