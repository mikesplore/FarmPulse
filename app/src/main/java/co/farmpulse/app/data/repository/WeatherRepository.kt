package co.farmpulse.app.data.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import co.farmpulse.app.data.local.db.WeatherDao
import co.farmpulse.app.data.local.entities.*
import co.farmpulse.app.data.remote.api.WeatherApiService
import co.farmpulse.app.data.remote.dto.*
import co.farmpulse.app.domain.model.WeatherData
import co.farmpulse.app.util.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

/**
 * Structured WeatherRepository. Fetches from network and falls back to granular Room tables.
 */
class WeatherRepository @Inject constructor(
    private val api: WeatherApiService,
    private val dao: WeatherDao,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) {

    data class WeatherResponseWithMeta(
        val response: WeatherGeoResponse,
        val cachedAt: Long? = null
    )

    suspend fun getFullWeather(
        lat: Double? = null,
        lon: Double? = null,
        ai: Boolean = true,
        lang: String = "en",
        units: String = "metric"
    ): Result<WeatherResponseWithMeta> = withContext(Dispatchers.IO) {
        if (networkMonitor.isOnline()) {
            try {
                Log.i("WeatherRepository", "Fetching weather from network (lat=$lat, lon=$lon, ai=$ai)")
                val response = if (lat != null && lon != null) {
                    api.getWeather(lat = lat, lon = lon, ai = ai, lang = lang, units = units)
                } else {
                    api.getWeatherByIp(ai = ai, lang = lang, units = units)
                }
                
                val finalLat = response.location?.lat ?: response.ipGeo?.lat ?: 0.0
                val finalLon = response.location?.lon ?: response.ipGeo?.lon ?: 0.0
                val city = reverseGeocode(finalLat, finalLon) ?: response.ipGeo?.city ?: "Unknown"

                saveToDatabase(response, city, finalLat, finalLon)
                Result.success(WeatherResponseWithMeta(response, null))
            } catch (e: Exception) {
                Log.w("WeatherRepository", "Network fetch failed, falling back to cache", e)
                loadFullFromCache()
            }
        } else {
            Log.i("WeatherRepository", "Device is offline, loading from cache")
            loadFullFromCache()
        }
    }

    private suspend fun saveToDatabase(response: WeatherGeoResponse, city: String, lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        
        // BACKFILL LOGIC: If 'current' data is incomplete, pull metrics from the first hour of forecast
        val firstHour = response.hourly?.firstOrNull()
        
        val current = CurrentWeatherEntity(
            city = city,
            region = response.ipGeo?.region ?: response.location?.timezone ?: "",
            lat = lat,
            lon = lon,
            temperature = response.current?.temperature ?: firstHour?.temperature,
            windSpeed = response.current?.windSpeed ?: firstHour?.windSpeed,
            windDirection = response.current?.windDirection,
            conditionCode = response.current?.conditionCode ?: firstHour?.conditionCode,
            icon = response.current?.icon ?: firstHour?.icon,
            iconPath = response.current?.iconPath ?: firstHour?.iconPath,
            // Prioritize current humidity/feels/uv/gust, but fall back to hourly if null
            humidity = response.current?.humidity ?: firstHour?.humidity,
            feelsLike = response.current?.feelsLike ?: firstHour?.feelsLike,
            uvIndex = response.current?.uvIndex ?: firstHour?.uvIndex,
            windGust = response.current?.windGust ?: firstHour?.windGust,
            aiSummary = response.aiSummary?.summary,
            fetchedAt = now
        )

        val hourly = (response.hourly ?: emptyList()).map {
            HourlyForecastEntity(
                time = it.time ?: "",
                temperature = it.temperature,
                precipitationProbability = it.precipitationProbability,
                windSpeed = it.windSpeed,
                conditionCode = it.conditionCode,
                icon = it.icon,
                iconPath = it.iconPath,
                humidity = it.humidity,
                feelsLike = it.feelsLike,
                windGust = it.windGust,
                uvIndex = it.uvIndex
            )
        }

        val daily = (response.daily ?: emptyList()).map {
            DailyForecastEntity(
                date = it.date ?: "",
                tempMin = it.tempMin,
                tempMax = it.tempMax,
                precipitationSum = it.precipitationSum,
                sunrise = it.sunrise,
                sunset = it.sunset,
                conditionCode = it.conditionCode,
                icon = it.icon,
                iconPath = it.iconPath,
                precipitationProbability = it.precipitationProbability,
                windMax = it.windMax
            )
        }

        dao.saveFullWeather(current, hourly, daily)
        Log.i("WeatherRepository", "Saved structured weather for $city (humidity=${current.humidity}, uv=${current.uvIndex})")
    }

    private suspend fun loadFullFromCache(): Result<WeatherResponseWithMeta> {
        val currentEntity = dao.getCurrentWeather() ?: return Result.failure(Exception("No cache"))
        val hourlyEntities = dao.getHourlyForecasts()
        val dailyEntities = dao.getDailyForecasts()

        val response = WeatherGeoResponse(
            location = LocationDto(lat = currentEntity.lat, lon = currentEntity.lon, timezone = currentEntity.region),
            ipGeo = IpGeoDto(city = currentEntity.city, region = currentEntity.region, lat = currentEntity.lat, lon = currentEntity.lon),
            current = CurrentWeatherDto(
                temperature = currentEntity.temperature,
                windSpeed = currentEntity.windSpeed,
                windDirection = currentEntity.windDirection,
                conditionCode = currentEntity.conditionCode,
                icon = currentEntity.icon,
                iconPath = currentEntity.iconPath,
                humidity = currentEntity.humidity,
                feelsLike = currentEntity.feelsLike,
                uvIndex = currentEntity.uvIndex,
                windGust = currentEntity.windGust
            ),
            hourly = hourlyEntities.map {
                HourlyForecastDto(
                    time = it.time,
                    temperature = it.temperature,
                    precipitationProbability = it.precipitationProbability,
                    windSpeed = it.windSpeed,
                    conditionCode = it.conditionCode,
                    icon = it.icon,
                    humidity = it.humidity,
                    feelsLike = it.feelsLike,
                    windGust = it.windGust,
                    uvIndex = it.uvIndex,
                    iconPath = it.iconPath
                )
            },
            daily = dailyEntities.map {
                DailyForecastDto(
                    date = it.date,
                    tempMin = it.tempMin,
                    tempMax = it.tempMax,
                    precipitationSum = it.precipitationSum,
                    sunrise = it.sunrise,
                    sunset = it.sunset,
                    conditionCode = it.conditionCode,
                    icon = it.icon,
                    precipitationProbability = it.precipitationProbability,
                    windMax = it.windMax,
                    iconPath = it.iconPath
                )
            },
            aiSummary = AiSummaryDto(summary = currentEntity.aiSummary)
        )

        return Result.success(WeatherResponseWithMeta(response, currentEntity.fetchedAt))
    }

    suspend fun getUsageStats(): Result<UsageResponse> = withContext(Dispatchers.IO) {
        if (networkMonitor.isOnline()) {
            try {
                val res = api.getUsageStats()
                dao.insertUsage(UsageEntity(
                    plan = res.plan, 
                    periodStart = res.period?.start, 
                    periodEnd = res.period?.end,
                    requestCount = res.period?.requestCount ?: 0, 
                    aiRequestCount = res.period?.aiRequestCount ?: 0,
                    limitRequests = res.limits?.requests ?: 0, 
                    limitAiRequests = res.limits?.aiRequests ?: 0,
                    maxDays = res.limits?.maxDays ?: 0, 
                    remainingRequests = res.remaining?.requests ?: 0,
                    remainingAiRequests = res.remaining?.aiRequests ?: 0, 
                    fetchedAt = System.currentTimeMillis()
                ))
                Result.success(res)
            } catch (e: Exception) { loadUsageFromCache() }
        } else { loadUsageFromCache() }
    }

    private suspend fun loadUsageFromCache(): Result<UsageResponse> {
        val u = dao.getUsage() ?: return Result.failure(Exception("No usage cache"))
        return Result.success(UsageResponse(
            plan = u.plan,
            period = UsagePeriodDto(u.periodStart, u.periodEnd, u.requestCount, u.aiRequestCount),
            limits = UsageLimitsDto(u.limitRequests, u.limitAiRequests, u.maxDays),
            remaining = UsageRemainingDto(u.remainingRequests, u.remainingAiRequests)
        ))
    }

    private fun reverseGeocode(lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.let { address ->
                address.locality ?: address.subAdminArea ?: address.adminArea
            }
        } catch (e: Exception) {
            Log.w("WeatherRepository", "Reverse geocoding failed", e)
            null
        }
    }

    suspend fun getWeather(): Result<WeatherData> {
        return getFullWeather().map { meta ->
            val response = meta.response
            WeatherData(
                city = response.ipGeo?.city ?: "Unknown",
                lat = response.location?.lat ?: 0.0,
                lon = response.location?.lon ?: 0.0,
                currentTemp = response.current?.temperature ?: 0.0
            )
        }
    }
}
