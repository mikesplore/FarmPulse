package co.farmpulse.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import co.farmpulse.app.data.local.entities.CachedTreeAnalysisEntity
import co.farmpulse.app.data.local.entities.CachedWeatherEntity

/**
 * Room database for FarmPulse. Includes cached weather (single row) and tree analyses.
 */
@Database(
    entities = [CachedWeatherEntity::class, CachedTreeAnalysisEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FarmPulseDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun treeAnalysisDao(): TreeAnalysisDao
}

