package co.farmpulse.app.presentation.scanner

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.repository.TreeRepository
import co.farmpulse.app.domain.model.TreeAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

data class ScannerUiState(
    val selectedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val result: TreeAnalysisResult? = null,
    val error: String? = null,
    val quotaUsed: Int = 2,
    val quotaLimit: Int = 5,
    val quotaResetsAt: String = "Jul 1",
    val county: String = "",
    val acres: String = ""
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: TreeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState

    private var cameraImageUri: Uri? = null

    fun onImageSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedImageUri = uri,
            result = null,
            error = null
        )
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
        cameraImageUri?.let {
            _uiState.value = _uiState.value.copy(
                selectedImageUri = it,
                result = null,
                error = null
            )
        }
    }

    fun onCountyChanged(newCounty: String) {
        _uiState.value = _uiState.value.copy(county = newCounty)
    }

    fun onAcresChanged(newAcres: String) {
        _uiState.value = _uiState.value.copy(acres = newAcres)
    }

    fun analyzeImage(context: Context, farmerId: String = "test-001", notes: String = "") {
        val uri = _uiState.value.selectedImageUri ?: return
        val county = _uiState.value.county
        val acres = _uiState.value.acres

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, error = null)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Unable to read image")
                val requestBody = bytes.toRequestBody("image/*".toMediaType())
                val imagePart = MultipartBody.Part.createFormData("image", "farm.jpg", requestBody)

                val result = repository.analyzeTrees(imagePart, farmerId, county, acres, notes)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        result = result.getOrNull(),
                        quotaUsed = _uiState.value.quotaUsed + 1
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        error = result.exceptionOrNull()?.message ?: "Analysis failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
}
