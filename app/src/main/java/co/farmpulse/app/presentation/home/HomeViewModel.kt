package co.farmpulse.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.local.prefs.UserPreferencesRepository
import co.farmpulse.app.data.remote.dto.CurrentWeatherDto
import co.farmpulse.app.data.remote.dto.HourlyForecastDto
import co.farmpulse.app.data.remote.dto.DailyForecastDto
import co.farmpulse.app.data.repository.WeatherRepository
import co.farmpulse.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDateTime

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
    val isLoadingAiSummary: Boolean = false,
    val aiEnabled: Boolean = true,
    val isOffline: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        observePreferences()
        observeNetwork()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnlineFlow.collectLatest { isOnline ->
                _uiState.value = _uiState.value.copy(isOffline = !isOnline)
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsRepository.userPreferencesFlow.collectLatest { prefs ->
                _uiState.value = _uiState.value.copy(aiEnabled = prefs.aiEnabled)
                loadWeather()
            }
        }
    }

    fun loadWeather() {
        viewModelScope.launch {
            val prefs = prefsRepository.userPreferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null,
                isLoadingAiSummary = prefs.aiEnabled
            )
            
            val res = repository.getFullWeather(
                ai = prefs.aiEnabled,
                lang = prefs.lang,
                units = prefs.units
            )
            
            if (res.isSuccess) {
                val value = res.getOrNull()
                if (value != null) {
                    val response = value.response
                    
                    val city = prefs.cityOverride.ifBlank {
                        response.ipGeo?.city ?: response.location?.country ?: "Unknown"
                    }
                    val region = response.ipGeo?.region ?: ""
                    
                    val nowHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
                    val filteredHourly = (response.hourly ?: emptyList()).filter { hour ->
                        try {
                            val t = LocalDateTime.parse(hour.time)
                            !t.isBefore(nowHour)
                        } catch (e: Exception) { 
                            false 
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        city = city,
                        region = region,
                        lat = response.location?.lat ?: response.ipGeo?.lat ?: 0.0,
                        lon = response.location?.lon ?: response.ipGeo?.lon ?: 0.0,
                        current = response.current,
                        hourly = filteredHourly,
                        daily = response.daily ?: emptyList(),
                        aiSummary = response.aiSummary?.summary,
                        isLoadingAiSummary = false,
                        isFromCache = value.cachedAt != null,
                        cachedAt = value.cachedAt
                    )
                    Log.i("HomeViewModel", "Loaded weather $city, Data: ${response.current}")
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Empty response")
                }
            } else {
                val ex = res.exceptionOrNull()
                Log.w("HomeViewModel", "Failed to load weather: ${ex?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    isLoadingAiSummary = false,
                    error = ex?.message ?: "Error"
                )
            }
        }
    }

    fun refreshWithAi() {
        loadWeather()
    }
}
