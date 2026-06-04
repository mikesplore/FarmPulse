package co.farmpulse.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached weather — single-row cache (id = 1)
 */
@Entity(tableName = "cached_weather")
data class CachedWeatherEntity(
    @PrimaryKey val id: Int = 1,
    val city: String,
    val region: String,
    val lat: Double,
    val lon: Double,
    val json: String,
    val fetchedAt: Long
)

