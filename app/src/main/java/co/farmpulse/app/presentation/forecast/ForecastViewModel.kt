package co.farmpulse.app.presentation.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.local.prefs.UserPreferencesRepository
import co.farmpulse.app.data.remote.dto.DailyForecastDto
import co.farmpulse.app.data.remote.dto.HourlyForecastDto
import co.farmpulse.app.data.repository.WeatherRepository
import co.farmpulse.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForecastUiState(
    val isLoading: Boolean = false,
    val hourly: List<HourlyForecastDto> = emptyList(),
    val daily: List<DailyForecastDto> = emptyList(),
    val locationLabel: String = "",
    val error: String? = null,
    val aiSummary: String? = null,
    val aiEnabled: Boolean = true,
    val isOffline: Boolean = false
)

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForecastUiState(isLoading = true))
    val uiState: StateFlow<ForecastUiState> = _uiState

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
                loadForecast()
            }
        }
    }

    fun loadForecast() {
        viewModelScope.launch {
            val prefs = prefsRepository.userPreferencesFlow.first()
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Handle coordinate overrides if set in settings
            val lat = prefs.latOverride.toDoubleOrNull()
            val lon = prefs.lonOverride.toDoubleOrNull()

            val res = repository.getFullWeather(
                lat = lat,
                lon = lon,
                ai = prefs.aiEnabled,
                lang = prefs.lang,
                units = prefs.units
            )
            
            if (res.isSuccess) {
                val v = res.getOrNull()
                val city = if (prefs.cityOverride.isNotBlank()) prefs.cityOverride else {
                    v?.response?.ipGeo?.city ?: v?.response?.location?.country ?: "Unknown"
                }
                val region = v?.response?.ipGeo?.region ?: ""
                val label = if (region.isNotBlank() && prefs.cityOverride.isBlank()) "$city, $region" else city
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hourly = v?.response?.hourly ?: emptyList(),
                    daily = v?.response?.daily ?: emptyList(),
                    locationLabel = label,
                    aiSummary = v?.response?.aiSummary?.summary
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
