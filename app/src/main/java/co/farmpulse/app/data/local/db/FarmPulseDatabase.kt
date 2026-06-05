package co.farmpulse.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import co.farmpulse.app.data.local.entities.*

/**
 * Room database for FarmPulse.
 * Version 2: Migrated from JSON blob cache to structured weather tables.
 */
@Database(
    entities = [
        CachedWeatherEntity::class,
        CachedTreeAnalysisEntity::class,
        CurrentWeatherEntity::class,
        HourlyForecastEntity::class,
        DailyForecastEntity::class,
        UsageEntity::class // Added missing UsageEntity
    ],
    version = 2,
    exportSchema = false
)
abstract class FarmPulseDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun treeAnalysisDao(): TreeAnalysisDao
}
