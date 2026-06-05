package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CurrentWeatherDto(
    @SerializedName("time") val time: String? = null,
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("wind_speed") val windSpeed: Double? = null,
    @SerializedName("wind_direction") val windDirection: Int? = null,
    @SerializedName("condition_code") val conditionCode: String? = null,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("icon_path") val iconPath: String? = null,
    @SerializedName("humidity") val humidity: Double? = null,
    @SerializedName("feels_like") val feelsLike: Double? = null,
    @SerializedName("uv_index") val uvIndex: Double? = null,
    @SerializedName("wind_gust") val windGust: Double? = null
)
