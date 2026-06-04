package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CurrentWeatherDto(
    val time: String?,
    val temperature: Double?,
    val wind_speed: Double?,
    val wind_direction: Int?,
    val condition_code: String?,
    val icon: String?,
    val icon_path: String?
)

