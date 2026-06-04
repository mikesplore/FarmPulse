package co.farmpulse.app.data.remote.dto

// DTO placeholders based on Phase 0. These will be refined in Phase 2.
import com.google.gson.annotations.SerializedName

data class WeatherGeoResponse(
    @SerializedName("location") val location: LocationDto?,
    @SerializedName("current") val current: CurrentWeatherDto?,
    @SerializedName("hourly") val hourly: List<HourlyForecastDto>?,
    @SerializedName("daily") val daily: List<DailyForecastDto>?,
    @SerializedName("client_geo") val clientGeo: ClientGeoDto?
)

data class LocationDto(
    val lat: Double,
    val lon: Double,
    val timezone: String,
    val country: String
)

data class ClientGeoDto(
    val country: String?,
    val ip_hash: String?
)

