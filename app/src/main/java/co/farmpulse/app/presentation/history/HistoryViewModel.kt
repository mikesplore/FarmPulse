package co.farmpulse.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.repository.TreeRepository
import co.farmpulse.app.domain.model.TreeAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = false,
    val items: List<TreeAnalysisResult> = emptyList(),
    val selectedItem: TreeAnalysisResult? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TreeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.getHistory().collect { list ->
                _uiState.update { it.copy(items = list) }
            }
        }
    }

    fun selectItem(item: TreeAnalysisResult?) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Simulate network sync
            kotlinx.coroutines.delay(1000)
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
