package co.farmpulse.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import co.farmpulse.app.data.local.entities.CachedTreeAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAnalysis(entity: CachedTreeAnalysisEntity)

    @Query("SELECT * FROM tree_analyses ORDER BY createdAt DESC")
    fun getAllAnalyses(): Flow<List<CachedTreeAnalysisEntity>>
}

