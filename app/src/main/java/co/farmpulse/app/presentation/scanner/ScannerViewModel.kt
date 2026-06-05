package co.farmpulse.app.presentation.scanner

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.repository.TreeRepository
import co.farmpulse.app.domain.model.TreeAnalysisResult
import co.farmpulse.app.util.NetworkMonitor
import co.farmpulse.app.util.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

data class ScannerUiState(
    val selectedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val isRefreshing: Boolean = false,
    val result: TreeAnalysisResult? = null,
    val error: String? = null,
    val quotaUsed: Int = 2,
    val quotaLimit: Int = 5,
    val quotaResetsAt: String = "Jul 1",
    val county: String = "",
    val acres: String = "",
    val isOffline: Boolean = false
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: TreeRepository,
    private val networkMonitor: NetworkMonitor,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState

    private var cameraImageUri: Uri? = null

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnlineFlow.collectLatest { isOnline ->
                _uiState.update { it.copy(isOffline = !isOnline) }
            }
        }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(
            selectedImageUri = uri,
            result = null,
            error = null
        ) }
    }

    fun prepareCameraUri(context: Context): Uri {
        val file = File(context.cacheDir, "camera_capture.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        cameraImageUri = uri
        return uri
    }

    fun onCameraImageCaptured() {
        cameraImageUri?.let { uri ->
            _uiState.update { it.copy(
                selectedImageUri = uri,
                result = null,
                error = null
            ) }
        }
    }

    fun refreshQuota() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(1000)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onCountyChanged(newCounty: String) {
        _uiState.update { it.copy(county = newCounty) }
    }

    fun onAcresChanged(newAcres: String) {
        _uiState.update { it.copy(acres = newAcres) }
    }

    fun analyzeImage(context: Context, farmerId: String = "test-001", notes: String = "") {
        val uri = _uiState.value.selectedImageUri ?: return
        val county = _uiState.value.county
        val acres = _uiState.value.acres

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Unable to read image")
                
                // Fix: Get specific MimeType instead of image/*
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                
                val fileName = when(mimeType) {
                    "image/png" -> "farm.png"
                    "image/webp" -> "farm.webp"
                    else -> "farm.jpg"
                }
                val imagePart = MultipartBody.Part.createFormData("image", fileName, requestBody)

                val result = repository.analyzeTrees(imagePart, farmerId, county, acres, notes)
                if (result.isSuccess) {
                    _uiState.update { it.copy(
                        isAnalyzing = false,
                        result = result.getOrNull(),
                        quotaUsed = it.quotaUsed + 1
                    ) }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Analysis failed"
                    _uiState.update { it.copy(isAnalyzing = false, error = errorMsg) }
                    snackbarManager.showMessage(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error occurred"
                _uiState.update { it.copy(isAnalyzing = false, error = errorMsg) }
                snackbarManager.showMessage(errorMsg)
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null) }
    }
}
