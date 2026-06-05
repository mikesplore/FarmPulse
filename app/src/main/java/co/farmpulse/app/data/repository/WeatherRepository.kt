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
        val discoveredCity: String? = null,
        val discoveredRegion: String? = null,
        val cachedAt: Long? = null
    )

    data class GeoInfo(val city: String?, val region: String?)

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
                
                var response: WeatherGeoResponse
                
                if (lat != null && lon != null) {
                    response = api.getWeather(lat = lat, lon = lon, ai = ai, lang = lang, units = units)
                } else {
                    val geoRes = api.getWeatherByIp(ai = ai, lang = lang, units = units)
                    val discoveredLat = geoRes.location?.lat ?: geoRes.ipGeo?.lat
                    val discoveredLon = geoRes.location?.lon ?: geoRes.ipGeo?.lon
                    
                    if (discoveredLat != null && discoveredLon != null) {
                        Log.i("WeatherRepository", "Location discovered via IP: $discoveredLat, $discoveredLon. Fetching full weather.")
                        val fullRes = api.getWeather(lat = discoveredLat, lon = discoveredLon, ai = ai, lang = lang, units = units)
                        response = fullRes.copy(ipGeo = geoRes.ipGeo)
                    } else {
                        response = geoRes
                    }
                }
                
                response = enrichCurrentWithHourlyData(response)

                val finalLat = response.location?.lat ?: response.ipGeo?.lat ?: 0.0
                val finalLon = response.location?.lon ?: response.ipGeo?.lon ?: 0.0
                
                // Get city and region from client-side Geocoder for exact location
                val geoInfo = reverseGeocodeExtended(finalLat, finalLon)
                val city = geoInfo.city ?: response.ipGeo?.city ?: response.location?.country ?: "Unknown"
                val region = geoInfo.region ?: response.ipGeo?.region ?: response.location?.timezone ?: ""

                saveToDatabase(response, city, region, finalLat, finalLon)
                Result.success(WeatherResponseWithMeta(response, city, region, null))
            } catch (e: Exception) {
                Log.w("WeatherRepository", "Network fetch failed, falling back to cache", e)
                loadFullFromCache()
            }
        } else {
            Log.i("WeatherRepository", "Device is offline, loading from cache")
            loadFullFromCache()
        }
    }

    private fun enrichCurrentWithHourlyData(response: WeatherGeoResponse): WeatherGeoResponse {
        val current = response.current ?: return response
        val hourly = response.hourly ?: return response
        
        val currentTimeStr = current.time ?: ""
        val hourPrefix = if (currentTimeStr.length >= 13) currentTimeStr.take(13) else ""
        
        val matchedHour = if (hourPrefix.isNotBlank()) {
            hourly.find { it.time?.startsWith(hourPrefix) == true } ?: hourly.firstOrNull()
        } else {
            hourly.firstOrNull()
        } ?: return response

        val enrichedCurrent = current.copy(
            humidity = current.humidity ?: matchedHour.humidity,
            feelsLike = current.feelsLike ?: matchedHour.feelsLike,
            uvIndex = current.uvIndex ?: matchedHour.uvIndex,
            windGust = current.windGust ?: matchedHour.windGust,
            temperature = current.temperature ?: matchedHour.temperature,
            windSpeed = current.windSpeed ?: matchedHour.windSpeed,
            conditionCode = current.conditionCode ?: matchedHour.conditionCode,
            icon = current.icon ?: matchedHour.icon,
            iconPath = current.iconPath ?: matchedHour.iconPath
        )
        
        return response.copy(current = enrichedCurrent)
    }

    private suspend fun saveToDatabase(response: WeatherGeoResponse, city: String, region: String, lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        
        val current = CurrentWeatherEntity(
            city = city,
            region = region,
            lat = lat,
            lon = lon,
            temperature = response.current?.temperature,
            windSpeed = response.current?.windSpeed,
            windDirection = response.current?.windDirection,
            conditionCode = response.current?.conditionCode,
            icon = response.current?.icon,
            iconPath = response.current?.iconPath,
            humidity = response.current?.humidity,
            feelsLike = response.current?.feelsLike,
            uvIndex = response.current?.uvIndex,
            windGust = response.current?.windGust,
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
        Log.i("WeatherRepository", "Saved enriched weather for $city to cache")
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

        return Result.success(WeatherResponseWithMeta(response, currentEntity.city, currentEntity.region, currentEntity.fetchedAt))
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

    private fun reverseGeocodeExtended(lat: Double, lon: Double): GeoInfo {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.let { address ->
                GeoInfo(
                    city = address.locality ?: address.subAdminArea ?: address.adminArea,
                    region = address.adminArea ?: address.subAdminArea
                )
            } ?: GeoInfo(null, null)
        } catch (e: Exception) {
            Log.w("WeatherRepository", "Reverse geocoding failed", e)
            GeoInfo(null, null)
        }
    }
}
