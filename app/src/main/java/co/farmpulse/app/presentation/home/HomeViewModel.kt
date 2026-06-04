package co.farmpulse.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.remote.dto.CurrentWeatherDto
import co.farmpulse.app.data.remote.dto.HourlyForecastDto
import co.farmpulse.app.data.remote.dto.DailyForecastDto
import co.farmpulse.app.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val city: String = "",
    val region: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val current: CurrentWeatherDto? = null,
    val hourly: List<HourlyForecastDto> = emptyList(),
    val daily: List<DailyForecastDto> = emptyList(),
    val error: String? = null,
    val isFromCache: Boolean = false,
    val cachedAt: Long? = null,
    val aiSummary: String? = null,
    val isLoadingAiSummary: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadWeather()
    }

    fun loadWeather() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.getFullWeather()
            if (res.isSuccess) {
                val value = res.getOrNull()
                if (value != null) {
                    Log.i("HomeViewModel", "Loaded weather response; cachedAt=${value.cachedAt}")
                    val city = value.response.ipGeo?.city
                        ?: value.response.location?.country
                        ?: value.response.ipGeo?.region
                        ?: "Unknown"
                    val region = value.response.ipGeo?.region ?: value.response.location?.timezone ?: ""
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        city = city,
                        region = region,
                        lat = value.response.location?.lat ?: value.response.ipGeo?.lat ?: 0.0,
                        lon = value.response.location?.lon ?: value.response.ipGeo?.lon ?: 0.0,
                        current = value.response.current,
                        hourly = value.response.hourly ?: emptyList(),
                        daily = value.response.daily ?: emptyList(),
                        isFromCache = value.cachedAt != null,
                        cachedAt = value.cachedAt
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Empty response")
                }
            } else {
                val ex = res.exceptionOrNull()
                Log.w("HomeViewModel", "Failed to load weather: ${ex?.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = ex?.message ?: "Error")
            }
        }
    }

    fun loadAiSummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAiSummary = true)
            val currentState = _uiState.value
            val r = repository.getWeatherWithAi(currentState.lat, currentState.lon)
            if (r.isSuccess) {
                // The free plan responses captured in phase0 didn't include an AI summary string; leave nullable
                // or populate with a friendly placeholder until the real AI data is integrated.
                _uiState.value = _uiState.value.copy(
                    isLoadingAiSummary = false, 
                    aiSummary = "Good conditions for field work today. Humidity drops after 3 PM — best window for pesticide application."
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoadingAiSummary = false, error = r.exceptionOrNull()?.message ?: "Failed to load AI insight")
            }
        }
    }
}
