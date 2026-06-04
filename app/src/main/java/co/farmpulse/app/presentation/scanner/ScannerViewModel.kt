package co.farmpulse.app.presentation.scanner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.repository.TreeRepository
import co.farmpulse.app.domain.model.TreeAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import javax.inject.Inject

data class ScannerUiState(
    val selectedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val result: TreeAnalysisResult? = null,
    val error: String? = null,
    val quotaUsed: Int = 0,
    val quotaLimit: Int = 5
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: TreeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState

    fun analyzeImage(context: Context, uri: Uri, farmerId: String = "test-001", county: String = "", acres: String = "1.0", notes: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, error = null)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Unable to read image")
                val requestBody = bytes.toRequestBody("image/*".toMediaType())
                val imagePart = MultipartBody.Part.createFormData("image", "farm.jpg", requestBody)

                val result = repository.analyzeTrees(imagePart, farmerId, county, acres, notes)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isAnalyzing = false, result = result.getOrNull())
                } else {
                    _uiState.value = _uiState.value.copy(isAnalyzing = false, error = result.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isAnalyzing = false, error = e.message)
            }
        }
    }
}

