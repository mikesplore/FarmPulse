package co.farmpulse.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserPreferences(
    val aiEnabled: Boolean,
    val lang: String,
    val units: String,
    val cityOverride: String,
    val latOverride: String,
    val lonOverride: String,
    val apiKey: String
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val LANG = stringPreferencesKey("lang")
        val UNITS = stringPreferencesKey("units")
        val CITY_OVERRIDE = stringPreferencesKey("city_override")
        val LAT_OVERRIDE = stringPreferencesKey("lat_override")
        val LON_OVERRIDE = stringPreferencesKey("lon_override")
        val API_KEY = stringPreferencesKey("api_key")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                aiEnabled = preferences[PreferencesKeys.AI_ENABLED] ?: true,
                lang = preferences[PreferencesKeys.LANG] ?: "en",
                units = preferences[PreferencesKeys.UNITS] ?: "metric",
                cityOverride = preferences[PreferencesKeys.CITY_OVERRIDE] ?: "",
                latOverride = preferences[PreferencesKeys.LAT_OVERRIDE] ?: "",
                lonOverride = preferences[PreferencesKeys.LON_OVERRIDE] ?: "",
                apiKey = preferences[PreferencesKeys.API_KEY] ?: ""
            )
        }

    fun getApiKeyBlocking(): String = runBlocking {
        context.dataStore.data.first()[PreferencesKeys.API_KEY] ?: ""
    }

    suspend fun updateAiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_ENABLED] = enabled
        }
    }

    suspend fun updateLang(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANG] = lang
        }
    }

    suspend fun updateUnits(units: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.UNITS] = units
        }
    }

    suspend fun updateLocationOverride(city: String, lat: String, lon: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CITY_OVERRIDE] = city
            preferences[PreferencesKeys.LAT_OVERRIDE] = lat
            preferences[PreferencesKeys.LON_OVERRIDE] = lon
        }
    }

    suspend fun updateApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = apiKey
        }
    }
}
