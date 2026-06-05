package co.farmpulse.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import co.farmpulse.app.data.local.entities.CurrentWeatherEntity
import co.farmpulse.app.data.local.entities.DailyForecastEntity
import co.farmpulse.app.data.local.entities.HourlyForecastEntity
import co.farmpulse.app.data.local.entities.UsageEntity

@Dao
interface WeatherDao {

    @Transaction
    suspend fun saveFullWeather(
        current: CurrentWeatherEntity,
        hourly: List<HourlyForecastEntity>,
        daily: List<DailyForecastEntity>
    ) {
        deleteCurrentWeather()
        deleteHourlyForecast()
        deleteDailyForecast()
        
        insertCurrentWeather(current)
        insertHourlyForecasts(hourly)
        insertDailyForecasts(daily)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(current: CurrentWeatherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyForecasts(hourly: List<HourlyForecastEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyForecasts(daily: List<DailyForecastEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: UsageEntity)

    @Query("DELETE FROM current_weather")
    suspend fun deleteCurrentWeather()

    @Query("DELETE FROM hourly_forecast")
    suspend fun deleteHourlyForecast()

    @Query("DELETE FROM daily_forecast")
    suspend fun deleteDailyForecast()

    @Query("SELECT * FROM current_weather LIMIT 1")
    suspend fun getCurrentWeather(): CurrentWeatherEntity?

    @Query("SELECT * FROM hourly_forecast")
    suspend fun getHourlyForecasts(): List<HourlyForecastEntity>

    @Query("SELECT * FROM daily_forecast")
    suspend fun getDailyForecasts(): List<DailyForecastEntity>

    @Query("SELECT * FROM api_usage WHERE id = 1")
    suspend fun getUsage(): UsageEntity?
}
