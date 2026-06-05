package co.farmpulse.app.presentation.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.remote.dto.DailyForecastDto
import co.farmpulse.app.data.remote.dto.HourlyForecastDto
import co.farmpulse.app.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForecastUiState(
    val isLoading: Boolean = false,
    val hourly: List<HourlyForecastDto> = emptyList(),
    val daily: List<DailyForecastDto> = emptyList(),
    val locationLabel: String = "",
    val error: String? = null
)

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForecastUiState(isLoading = true))
    val uiState: StateFlow<ForecastUiState> = _uiState

    init {
        loadForecast()
    }

    fun loadForecast() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.getFullWeather()
            if (res.isSuccess) {
                val v = res.getOrNull()
                val city = v?.response?.ipGeo?.city ?: v?.response?.location?.country ?: "Unknown"
                val region = v?.response?.ipGeo?.region ?: ""
                val label = if (region.isNotBlank()) "$city, $region" else city
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hourly = v?.response?.hourly ?: emptyList(),
                    daily = v?.response?.daily ?: emptyList(),
                    locationLabel = label
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = res.exceptionOrNull()?.message
                )
            }
        }
    }
}
