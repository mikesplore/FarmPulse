package co.farmpulse.app.presentation.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.local.prefs.UserPreferencesRepository
import co.farmpulse.app.data.remote.dto.DailyForecastDto
import co.farmpulse.app.data.remote.dto.HourlyForecastDto
import co.farmpulse.app.data.repository.WeatherRepository
import co.farmpulse.app.util.LocationService
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
    val isRefreshing: Boolean = false,
    val hourly: List<HourlyForecastDto> = emptyList(),
    val daily: List<DailyForecastDto> = emptyList(),
    val locationLabel: String = "",
    val error: String? = null,
    val aiSummary: String? = null,
    val aiEnabled: Boolean = true,
    val isOffline: Boolean = false,
    val isApiKeyMissing: Boolean = false
)

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForecastUiState(isLoading = false))
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
                _uiState.value = _uiState.value.copy(
                    aiEnabled = prefs.aiEnabled,
                    isApiKeyMissing = prefs.apiKey.isBlank()
                )
            }
        }
    }

    fun loadForecast(usePreciseLocation: Boolean = false, isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            val prefs = prefsRepository.userPreferencesFlow.first()
            _uiState.value = _uiState.value.copy(
                isLoading = !isPullToRefresh && _uiState.value.daily.isEmpty(),
                isRefreshing = isPullToRefresh,
                error = null,
                isApiKeyMissing = prefs.apiKey.isBlank()
            )
            
            // Handle coordinate overrides if set in settings
            var lat: Double? = prefs.latOverride.toDoubleOrNull() ?: prefs.lastLat
            var lon: Double? = prefs.lonOverride.toDoubleOrNull() ?: prefs.lastLon

            if (usePreciseLocation && prefs.latOverride.isBlank() && prefs.lonOverride.isBlank()) {
                val location = locationService.getCurrentLocation()
                if (location != null) {
                    lat = location.latitude
                    lon = location.longitude
                    // Persist for next time
                    prefsRepository.updateLastKnownLocation(lat, lon)
                }
            }

            val res = repository.getFullWeather(
                lat = lat,
                lon = lon,
                ai = prefs.aiEnabled,
                lang = prefs.lang,
                units = prefs.units
            )
            
            if (res.isSuccess) {
                val v = res.getOrNull()
                // Use the geocoded city from the client device (via repository)
                val city = if (prefs.cityOverride.isNotBlank()) prefs.cityOverride else {
                    v?.discoveredCity ?: v?.response?.ipGeo?.city ?: v?.response?.location?.country ?: "Unknown"
                }
                val region = v?.response?.ipGeo?.region ?: ""
                val label = if (region.isNotBlank() && prefs.cityOverride.isBlank()) "$city, $region" else city
                
                // Persist discovered coordinates if we don't have overrides
                if (prefs.latOverride.isBlank() && prefs.lonOverride.isBlank()) {
                    val finalLat = v?.response?.location?.lat ?: v?.response?.ipGeo?.lat
                    val finalLon = v?.response?.location?.lon ?: v?.response?.ipGeo?.lon
                    if (finalLat != null && finalLon != null) {
                        prefsRepository.updateLastKnownLocation(finalLat, finalLon)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    hourly = v?.response?.hourly ?: emptyList(),
                    daily = v?.response?.daily ?: emptyList(),
                    locationLabel = label,
                    aiSummary = v?.response?.aiSummary?.summary
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    isRefreshing = false,
                    error = res.exceptionOrNull()?.message
                )
            }
        }
    }
}
