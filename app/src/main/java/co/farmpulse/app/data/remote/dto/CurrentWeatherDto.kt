package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CurrentWeatherDto(
    @SerializedName("time") val time: String? = null,
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("wind_speed") val windSpeed: Double? = null,
    @SerializedName("wind_direction") val windDirection: Double? = null,
    @SerializedName("condition_code") val conditionCode: String? = null,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("icon_path") val iconPath: String? = null
)
