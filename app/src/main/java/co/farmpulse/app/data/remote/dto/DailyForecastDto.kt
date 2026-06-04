package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DailyForecastDto(
    val date: String?,
    val temp_min: Double?,
    val temp_max: Double?,
    val precipitation_sum: Double?,
    val sunrise: String?,
    val sunset: String?,
    val condition_code: String?,
    val icon: String?,
    val precipitation_probability: Int?,
    val wind_max: Double?,
    val icon_path: String?
)

