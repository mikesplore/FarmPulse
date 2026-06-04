package co.farmpulse.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_analyses")
data class CachedTreeAnalysisEntity(
    @PrimaryKey val analysisId: String,
    val json: String,
    val createdAt: Long
)

