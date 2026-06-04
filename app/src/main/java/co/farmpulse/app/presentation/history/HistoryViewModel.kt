package co.farmpulse.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.repository.TreeRepository
import co.farmpulse.app.domain.model.TreeAnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TreeRepository
) : ViewModel() {

    private val _history = MutableStateFlow<List<TreeAnalysisResult>>(emptyList())
    val history: StateFlow<List<TreeAnalysisResult>> = _history

    init {
        viewModelScope.launch {
            repository.getHistory().collect { list ->
                _history.value = list
            }
        }
    }
}

