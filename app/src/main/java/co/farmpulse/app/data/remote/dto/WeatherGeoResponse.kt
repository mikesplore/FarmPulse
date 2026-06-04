package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeatherGeoResponse(
    @SerializedName("location") val location: LocationDto? = null,
    @SerializedName("current") val current: CurrentWeatherDto? = null,
    @SerializedName("hourly") val hourly: List<HourlyForecastDto>? = null,
    @SerializedName("daily") val daily: List<DailyForecastDto>? = null,
    @SerializedName("client_geo") val clientGeo: ClientGeoDto? = null,
    @SerializedName("ip_geo") val ipGeo: IpGeoDto? = null
)

data class LocationDto(
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lon") val lon: Double? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("requested_lat") val requestedLat: Double? = null,
    @SerializedName("requested_lon") val requestedLon: Double? = null,
    @SerializedName("country") val country: String? = null
)

data class ClientGeoDto(
    @SerializedName("country") val country: String? = null,
    @SerializedName("ip_hash") val ipHash: String? = null
)

data class IpGeoDto(
    @SerializedName("country") val country: String? = null,
    @SerializedName("region") val region: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lon") val lon: Double? = null,
    @SerializedName("asn") val asn: Int? = null,
    @SerializedName("org") val org: String? = null,
    @SerializedName("ip_hash") val ipHash: String? = null,
    @SerializedName("source") val source: String? = null
)

typealias CurrentWeatherResponse = WeatherGeoResponse
typealias DailyForecastResponse = WeatherGeoResponse
typealias HourlyForecastResponse = WeatherGeoResponse

