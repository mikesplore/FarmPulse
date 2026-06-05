package co.farmpulse.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.local.prefs.UserPreferencesRepository
import co.farmpulse.app.data.remote.dto.CurrentWeatherDto
import co.farmpulse.app.data.remote.dto.HourlyForecastDto
import co.farmpulse.app.data.remote.dto.DailyForecastDto
import co.farmpulse.app.data.repository.WeatherRepository
import co.farmpulse.app.util.LocationService
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
    val isRefreshing: Boolean = false,
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
    val isOffline: Boolean = false,
    val isApiKeyMissing: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
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
                _uiState.value = _uiState.value.copy(
                    aiEnabled = prefs.aiEnabled,
                    isApiKeyMissing = prefs.apiKey.isBlank()
                )
            }
        }
    }

    fun loadWeather(usePreciseLocation: Boolean = false, isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            val prefs = prefsRepository.userPreferencesFlow.first()
            
            val showFullLoading = !isPullToRefresh && _uiState.value.current == null
            
            _uiState.value = _uiState.value.copy(
                isLoading = showFullLoading,
                isRefreshing = isPullToRefresh,
                error = null,
                isApiKeyMissing = prefs.apiKey.isBlank()
            )

            var lat: Double? = prefs.latOverride.toDoubleOrNull() ?: prefs.lastLat
            var lon: Double? = prefs.lonOverride.toDoubleOrNull() ?: prefs.lastLon

            if (usePreciseLocation && prefs.latOverride.isBlank() && prefs.lonOverride.isBlank()) {
                val location = locationService.getCurrentLocation()
                if (location != null) {
                    lat = location.latitude
                    lon = location.longitude
                    Log.i("HomeViewModel", "Using precise location: $lat, $lon")
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
                val value = res.getOrNull()
                if (value != null) {
                    val response = value.response
                    
                    // Display the client-side Geocoded info (exact city and region)
                    val city = if (prefs.cityOverride.isNotBlank()) prefs.cityOverride else {
                        value.discoveredCity ?: "Unknown"
                    }
                    val region = if (prefs.cityOverride.isNotBlank()) "" else {
                        value.discoveredRegion ?: ""
                    }
                    
                    val nowHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
                    val filteredHourly = (response.hourly ?: emptyList()).filter { hour ->
                        try {
                            val t = LocalDateTime.parse(hour.time)
                            !t.isBefore(nowHour)
                        } catch (e: Exception) { 
                            false 
                        }
                    }

                    if (prefs.latOverride.isBlank() && prefs.lonOverride.isBlank()) {
                        val discLat = response.location?.lat ?: response.ipGeo?.lat
                        val discLon = response.location?.lon ?: response.ipGeo?.lon
                        if (discLat != null && discLon != null) {
                            prefsRepository.updateLastKnownLocation(discLat, discLon)
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
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
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, error = "Empty response")
                }
            } else {
                val ex = res.exceptionOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    isRefreshing = false,
                    isLoadingAiSummary = false,
                    error = ex?.message ?: "Error"
                )
            }
        }
    }

    fun refreshWithAi() {
        loadWeather(isPullToRefresh = true)
    }
}
