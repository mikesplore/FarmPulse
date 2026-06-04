package co.farmpulse.app.data.repository

import co.farmpulse.app.data.local.db.WeatherDao
import javax.inject.Inject
import co.farmpulse.app.data.local.entities.CachedWeatherEntity
import co.farmpulse.app.data.remote.api.WeatherApiService
import co.farmpulse.app.data.remote.dto.WeatherGeoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import co.farmpulse.app.domain.model.WeatherData
import co.farmpulse.app.util.NetworkMonitor
import com.google.gson.Gson
import android.util.Log

/**
 * Offline-first WeatherRepository. Fetches from network when online and falls back to Room cache.
 */
class WeatherRepository @Inject constructor(
    private val api: WeatherApiService,
    private val dao: WeatherDao,
    private val networkMonitor: NetworkMonitor
) {
    private val gson = Gson()

    data class WeatherResponseWithMeta(
        val response: WeatherGeoResponse,
        val cachedAt: Long? = null
    )

    suspend fun getFullWeather(): Result<WeatherResponseWithMeta> = withContext(Dispatchers.IO) {
        if (networkMonitor.isOnline()) {
            try {
                val response = api.getWeatherByIp()
                val lat = response.location?.lat ?: response.ipGeo?.lat ?: 0.0
                val lon = response.location?.lon ?: response.ipGeo?.lon ?: 0.0
                val city = response.ipGeo?.city ?: "Unknown"

                val json = gson.toJson(response)
                val entity = CachedWeatherEntity(
                    city = city,
                    region = response.ipGeo?.region ?: "",
                    lat = lat,
                    lon = lon,
                    json = json,
                    fetchedAt = System.currentTimeMillis()
                )
                try {
                    dao.saveWeather(entity)
                    Log.i("WeatherRepository", "Saved weather cache for city=${entity.city} at ${entity.fetchedAt}")
                    // Read back immediately to verify persistence
                    try {
                        val cached = dao.getCachedWeather()
                        if (cached != null) {
                            Log.i("WeatherRepository", "Verified cached entry id=${cached.id} city=${cached.city} fetchedAt=${cached.fetchedAt}")
                        } else {
                            Log.w("WeatherRepository", "Cache write verified: no entry found after save")
                        }
                    } catch (e: Exception) {
                        Log.e("WeatherRepository", "Failed to read back cache after save", e)
                    }
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Failed to save weather cache", e)
                }

                Result.success(WeatherResponseWithMeta(response, null))
            } catch (e: Exception) {
                // network error -> try load from cache
                Log.w("WeatherRepository", "Network fetch failed, falling back to cache", e)
                loadFullFromCache()
            }
        } else {
            loadFullFromCache()
        }
    }

    private suspend fun loadFullFromCache(): Result<WeatherResponseWithMeta> = withContext(Dispatchers.IO) {
        val cached = dao.getCachedWeather()
        if (cached != null) {
            try {
                val response = gson.fromJson(cached.json, WeatherGeoResponse::class.java)
                Log.i("WeatherRepository", "Loaded weather from cache city=${cached.city} fetchedAt=${cached.fetchedAt}")
                Result.success(WeatherResponseWithMeta(response, cached.fetchedAt))
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Failed to parse cached weather json", e)
                Result.failure(Exception("Failed to parse cached weather"))
            }
        } else {
            Log.i("WeatherRepository", "No cached weather found in DB")
            Result.failure(Exception("No cached data available"))
        }
    }

    suspend fun getWeatherWithAi(lat: Double, lon: Double): Result<WeatherResponseWithMeta> = withContext(Dispatchers.IO) {
        try {
            val response = api.getWeatherWithAiSummary(lat, lon)
            // do not cache AI responses to avoid quota misuse — but we can cache same as normal
            Result.success(WeatherResponseWithMeta(response, null))
        } catch (e: Exception) {
            // fallback to cache if available
            loadFullFromCache()
        }
    }

    /**
     * Return raw cached JSON string if present (used for debugging).
     */
    suspend fun getCachedJson(): String? = withContext(Dispatchers.IO) {
        try {
            val cached = dao.getCachedWeather()
            cached?.json
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to read cached json", e)
            null
        }
    }

    suspend fun getWeather(): Result<WeatherData> {
        return if (networkMonitor.isOnline()) {
            try {
                val response: WeatherGeoResponse = api.getWeatherByIp()
                val lat = response.location?.lat ?: response.ipGeo?.lat ?: 0.0
                val lon = response.location?.lon ?: response.ipGeo?.lon ?: 0.0
                val city = response.ipGeo?.city ?: "Unknown"

                val json = gson.toJson(response)
                val entity = CachedWeatherEntity(
                    city = city,
                    region = response.ipGeo?.region ?: "",
                    lat = lat,
                    lon = lon,
                    json = json,
                    fetchedAt = System.currentTimeMillis()
                )
                dao.saveWeather(entity)

                val domain = WeatherData(
                    city = city,
                    lat = lat,
                    lon = lon,
                    currentTemp = response.current?.temperature ?: 0.0
                )
                Log.i("WeatherRepository", "Saved weather cache (getWeather) for city=${entity.city}")
                try {
                    val cached = dao.getCachedWeather()
                    if (cached != null) {
                        Log.i("WeatherRepository", "Verified cached entry (getWeather) id=${cached.id} city=${cached.city} fetchedAt=${cached.fetchedAt}")
                    } else {
                        Log.w("WeatherRepository", "Cache write (getWeather) verified: no entry found after save")
                    }
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Failed to read back cache after save (getWeather)", e)
                }
                Result.success(domain)
            } catch (e: Exception) {
                loadFromCache()
            }
        } else {
            loadFromCache()
        }
    }

    private suspend fun loadFromCache(): Result<WeatherData> {
        val cached = dao.getCachedWeather()
        return if (cached != null) {
            try {
                val response = gson.fromJson(cached.json, WeatherGeoResponse::class.java)
                val lat = response.location?.lat ?: response.ipGeo?.lat ?: cached.lat
                val lon = response.location?.lon ?: response.ipGeo?.lon ?: cached.lon
                val city = response.ipGeo?.city ?: cached.city
                val domain = WeatherData(
                    city = city,
                    lat = lat,
                    lon = lon,
                    currentTemp = response.current?.temperature ?: 0.0
                )
                Result.success(domain)
            } catch (e: Exception) {
                Result.failure(Exception("Failed to parse cached weather"))
            }
        } else {
            Result.failure(Exception("No cached data available"))
        }
    }
}
