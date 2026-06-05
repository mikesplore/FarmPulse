package co.farmpulse.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_weather")
data class CurrentWeatherEntity(
    @PrimaryKey val id: Int = 1,
    val city: String,
    val region: String,
    val lat: Double,
    val lon: Double,
    val temperature: Double?,
    val windSpeed: Double?,
    val windDirection: Int?,
    val conditionCode: String?,
    val icon: String?,
    val iconPath: String?,
    val humidity: Double?,
    val feelsLike: Double?,
    val uvIndex: Double?,
    val windGust: Double?,
    val aiSummary: String?,
    val fetchedAt: Long
)

@Entity(tableName = "hourly_forecast")
data class HourlyForecastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: String,
    val temperature: Double?,
    val precipitationProbability: Double?,
    val windSpeed: Double?,
    val conditionCode: String?,
    val icon: String?,
    val iconPath: String?,
    val humidity: Double?,
    val feelsLike: Double?,
    val windGust: Double?,
    val uvIndex: Double?
)

@Entity(tableName = "daily_forecast")
data class DailyForecastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val tempMin: Double?,
    val tempMax: Double?,
    val precipitationSum: Double?,
    val sunrise: String?,
    val sunset: String?,
    val conditionCode: String?,
    val icon: String?,
    val iconPath: String?,
    val precipitationProbability: Double?,
    val windMax: Double?
)

@Entity(tableName = "api_usage")
data class UsageEntity(
    @PrimaryKey val id: Int = 1,
    val plan: String?,
    val periodStart: String?,
    val periodEnd: String?,
    val requestCount: Int,
    val aiRequestCount: Int,
    val limitRequests: Int,
    val limitAiRequests: Int,
    val maxDays: Int,
    val remainingRequests: Int,
    val remainingAiRequests: Int,
    val fetchedAt: Long
)
