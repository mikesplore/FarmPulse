package co.farmpulse.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import co.farmpulse.app.data.local.entities.CachedWeatherEntity

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeather(entity: CachedWeatherEntity)

    @Query("SELECT * FROM cached_weather WHERE id = 1")
    suspend fun getCachedWeather(): CachedWeatherEntity?
}

