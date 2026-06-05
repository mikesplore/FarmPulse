package co.farmpulse.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.farmpulse.app.data.local.prefs.UserPreferencesRepository
import co.farmpulse.app.data.repository.TreeRepository
import co.farmpulse.app.data.repository.WeatherRepository
import co.farmpulse.app.util.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // Weather API Usage
    val isLoadingUsage:      Boolean = true,
    val requestsUsed:        Int     = 0,
    val requestsLimit:       Int     = 1000,
    val requestsRemaining:   Int     = 1000,
    val aiRequestsUsed:      Int     = 0,
    val aiRequestsLimit:     Int     = 200,
    val aiRequestsRemaining: Int     = 200,
    val planName:            String  = "free",
    val maxDays:             Int     = 7,
    val periodEnd:           String? = null,

    // Farm Scanner Usage
    val treeQuotaUsed: Int = 0,
    val treeQuotaLimit: Int = 0,
    val treeQuotaResetsAt: String = "",

    // Preferences (persisted in DataStore)
    val aiEnabled:    Boolean = true,
    val lang:         String  = "en",   // "en" | "sw"
    val units:        String  = "metric",
    val apiKey:       String  = "",

    // Location override
    val ipCity:       String? = null,
    val cityOverride: String  = "",
    val latOverride:  String  = "",
    val lonOverride:  String  = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val treeRepository: TreeRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUsage()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsRepository.userPreferencesFlow.collectLatest { prefs ->
                _uiState.update { it.copy(
                    aiEnabled = prefs.aiEnabled,
                    lang = prefs.lang,
                    units = prefs.units,
                    cityOverride = prefs.cityOverride,
                    latOverride = prefs.latOverride,
                    lonOverride = prefs.lonOverride,
                    apiKey = prefs.apiKey
                ) }
            }
        }
    }

    /**
     * Refreshes API usage statistics and the auto-detected location info.
     */
    fun loadUsage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUsage = true) }
            
            // 1. Fetch weather usage statistics
            val res = repository.getUsageStats()
            
            // 2. Fetch tree analysis quota
            val treeRes = treeRepository.getQuota()
            
            // 3. Also fetch weather info to get detected city
            val weatherRes = repository.getFullWeather()
            val detectedCity = weatherRes.getOrNull()?.response?.ipGeo?.city

            _uiState.update { currentState ->
                var nextState = currentState.copy(
                    isLoadingUsage = false,
                    ipCity = detectedCity ?: currentState.ipCity
                )

                if (res.isSuccess) {
                    val usage = res.getOrNull()
                    if (usage != null) {
                        nextState = nextState.copy(
                            requestsUsed = usage.period?.requestCount ?: 0,
                            requestsLimit = usage.limits?.requests ?: 1000,
                            requestsRemaining = usage.remaining?.requests ?: 0,
                            aiRequestsUsed = usage.period?.aiRequestCount ?: 0,
                            aiRequestsLimit = usage.limits?.aiRequests ?: 200,
                            aiRequestsRemaining = usage.remaining?.aiRequests ?: 0,
                            planName = usage.plan ?: "free",
                            maxDays = usage.limits?.maxDays ?: 7,
                            periodEnd = usage.period?.end
                        )
                    }
                } else if (currentState.apiKey.isNotBlank()) {
                    snackbarManager.showMessage(res.exceptionOrNull()?.message ?: "Failed to load weather usage")
                }

                if (treeRes.isSuccess) {
                    val quota = treeRes.getOrNull()
                    nextState = nextState.copy(
                        treeQuotaUsed = quota?.used ?: 0,
                        treeQuotaLimit = quota?.limit ?: 0,
                        treeQuotaResetsAt = quota?.resetsAt ?: ""
                    )
                }

                nextState
            }
        }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.updateAiEnabled(enabled)
        }
    }

    fun setLang(lang: String) {
        viewModelScope.launch {
            prefsRepository.updateLang(lang)
        }
    }

    fun setUnits(units: String) {
        viewModelScope.launch {
            prefsRepository.updateUnits(units)
        }
    }

    fun setApiKey(apiKey: String) {
        viewModelScope.launch {
            prefsRepository.updateApiKey(apiKey)
            // Reload usage when API key changes
            loadUsage()
        }
    }

    fun setCityOverride(city: String) {
        _uiState.update { it.copy(cityOverride = city) }
        saveLocationOverride()
    }

    fun setLatOverride(lat: String) {
        _uiState.update { it.copy(latOverride = lat) }
        saveLocationOverride()
    }

    fun setLonOverride(lon: String) {
        _uiState.update { it.copy(lonOverride = lon) }
        saveLocationOverride()
    }

    private fun saveLocationOverride() {
        viewModelScope.launch {
            val s = _uiState.value
            prefsRepository.updateLocationOverride(s.cityOverride, s.latOverride, s.lonOverride)
        }
    }
}
