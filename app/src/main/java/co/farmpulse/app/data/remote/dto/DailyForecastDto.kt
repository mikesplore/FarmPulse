package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DailyForecastDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("temp_min") val tempMin: Double? = null,
    @SerializedName("temp_max") val tempMax: Double? = null,
    @SerializedName("precipitation_sum") val precipitationSum: Double? = null,
    @SerializedName("sunrise") val sunrise: String? = null,
    @SerializedName("sunset") val sunset: String? = null,
    @SerializedName("condition_code") val conditionCode: String? = null,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("precipitation_probability") val precipitationProbability: Double? = null,
    @SerializedName("wind_max") val windMax: Double? = null,
    @SerializedName("icon_path") val iconPath: String? = null
)
