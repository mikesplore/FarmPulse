package co.farmpulse.app.data.remote.api

import co.farmpulse.app.data.remote.dto.UsageResponse
import co.farmpulse.app.data.remote.dto.WeatherGeoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/weather-geo")
    suspend fun getWeatherByIp(
        @Query("ip")    ip:    String  = "auto",
        @Query("days")  days:  Int     = 7,
        @Query("ai")    ai:    Boolean = true,
        @Query("units") units: String  = "metric",
        @Query("lang")  lang:  String  = "en"
    ): WeatherGeoResponse


    @GET("v1/weather")
    suspend fun getWeather(
        @Query("lat")   lat:   Double,
        @Query("lon")   lon:   Double,
        @Query("days")  days:  Int     = 7,
        @Query("ai")    ai:    Boolean = true,
        @Query("units") units: String  = "metric",
        @Query("lang")  lang:  String  = "en"
    ): WeatherGeoResponse

    @GET("v1/usage")
    suspend fun getUsageStats(): UsageResponse
}
