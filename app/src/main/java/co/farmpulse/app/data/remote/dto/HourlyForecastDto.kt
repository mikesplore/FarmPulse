package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HourlyForecastDto(
    val time: String?,
    val temperature: Double?,
    val precipitation_probability: Int?,
    val wind_speed: Double?,
    val condition_code: String?,
    val icon: String?,
    val humidity: Int?,
    val feels_like: Double?,
    val wind_gust: Double?,
    val uv_index: Int?,
    val icon_path: String?
)

