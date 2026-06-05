package co.farmpulse.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.local.prefs.UserPreferencesRepository
import co.farmpulse.app.data.repository.TreeRepository
import co.farmpulse.app.domain.model.TreeAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<TreeAnalysisResult> = emptyList(),
    val selectedItem: TreeAnalysisResult? = null,
    val isApiKeyMissing: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TreeRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
        observePreferences()
        // Initial sync on startup
        refreshHistory(isInitialLoad = true)
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.getHistory().collect { list ->
                _uiState.update { it.copy(items = list) }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsRepository.userPreferencesFlow.collectLatest { prefs ->
                _uiState.update { it.copy(isApiKeyMissing = prefs.apiKey.isBlank()) }
            }
        }
    }

    fun selectItem(item: TreeAnalysisResult?) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun refreshHistory(isInitialLoad: Boolean = false) {
        viewModelScope.launch {
            if (isInitialLoad) {
                _uiState.update { it.copy(isLoading = it.items.isEmpty()) }
            } else {
                _uiState.update { it.copy(isRefreshing = true) }
            }

            // Call the actual sync method from repository
            repository.syncHistory()

            _uiState.update { 
                it.copy(
                    isLoading = false,
                    isRefreshing = false
                ) 
            }
        }
    }
}
