package ru.musikkk.player.feature.uploads

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.musikkk.player.data.upload.UploadRepository
import ru.musikkk.player.domain.upload.UploadInfo

data class UploadsUiState(
    val uploads: List<UploadInfo> = emptyList(),
)

@HiltViewModel
class UploadsViewModel @Inject constructor(
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    val state: StateFlow<UploadsUiState> = uploadRepository.observeAll()
        .map { UploadsUiState(uploads = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = UploadsUiState(),
        )

    /** Ставит набор файлов из SAF-пикера в очередь на аплоад. */
    fun enqueue(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            for (uri in uris) {
                uploadRepository.enqueue(uri)
            }
        }
    }

    fun cancel(uploadId: String) {
        viewModelScope.launch { uploadRepository.cancel(uploadId) }
    }

    fun retry(uploadId: String) {
        viewModelScope.launch { uploadRepository.retry(uploadId) }
    }

    fun remove(uploadId: String) {
        viewModelScope.launch { uploadRepository.remove(uploadId) }
    }

    fun clearCompleted() {
        viewModelScope.launch { uploadRepository.clearCompleted() }
    }
}
