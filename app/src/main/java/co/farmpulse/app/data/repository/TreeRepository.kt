package co.farmpulse.app.data.repository

import co.farmpulse.app.data.local.db.TreeAnalysisDao
import javax.inject.Inject
import co.farmpulse.app.data.local.entities.CachedTreeAnalysisEntity
import co.farmpulse.app.data.remote.api.TreeApiService
import co.farmpulse.app.data.remote.dto.TreeAnalysisResponse
import co.farmpulse.app.domain.model.TreeAnalysisResult
import co.farmpulse.app.util.NetworkMonitor
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

/**
 * Repository for tree analysis operations. Saves results to Room for history.
 */
class TreeRepository @Inject constructor(
    private val api: TreeApiService,
    private val dao: TreeAnalysisDao,
    private val networkMonitor: NetworkMonitor
) {
    private val gson = Gson()

    suspend fun analyzeTrees(
        imagePart: MultipartBody.Part,
        farmerId: String,
        county: String,
        landAcres: String,
        notes: String
    ): Result<TreeAnalysisResult> {
        if (!networkMonitor.isOnline()) {
            return Result.failure(Exception("No network available"))
        }

        return try {
            val mediaType = "text/plain".toMediaTypeOrNull()
            val farmerRb: RequestBody = farmerId.toRequestBody(mediaType)
            val countyRb: RequestBody = county.toRequestBody(mediaType)
            val landRb: RequestBody = (landAcres.toDoubleOrNull() ?: 0.0).toString().toRequestBody(mediaType)
            val notesRb: RequestBody = notes.toRequestBody(mediaType)

            val response: TreeAnalysisResponse = api.analyzeTrees(
                imagePart,
                farmerId = farmerRb,
                county = countyRb,
                landAcres = landRb,
                notes = notesRb
            )

            val analysisId = response.analysisId ?: java.util.UUID.randomUUID().toString()
            val json = gson.toJson(response)
            val entity = CachedTreeAnalysisEntity(
                analysisId = analysisId,
                json = json,
                createdAt = System.currentTimeMillis()
            )
            dao.saveAnalysis(entity)

            val domain = TreeAnalysisResult(
                analysisId = analysisId,
                totalTreeCount = response.totalTreeCount ?: 0,
                confidenceScore = response.confidenceScore ?: 0.0
            )

            Result.success(domain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getHistory(): Flow<List<TreeAnalysisResult>> {
        return dao.getAllAnalyses().map { list ->
            list.mapNotNull { entity ->
                try {
                    val dto = gson.fromJson(entity.json, TreeAnalysisResponse::class.java)
                    TreeAnalysisResult(
                        analysisId = dto.analysisId ?: entity.analysisId,
                        totalTreeCount = dto.totalTreeCount ?: 0,
                        confidenceScore = dto.confidenceScore ?: 0.0
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
