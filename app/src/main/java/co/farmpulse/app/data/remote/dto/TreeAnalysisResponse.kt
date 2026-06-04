package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TreeAnalysisResponse(
    @SerializedName("analysis_id") val analysisId: String?,
    val timestamp: String?,
    @SerializedName("farmer_id") val farmerId: String?,
    val county: String?,
    val location: Any?,
    @SerializedName("land_acres") val landAcres: Double?,
    @SerializedName("total_tree_count") val totalTreeCount: Int?,
    @SerializedName("confidence_score") val confidenceScore: Double?,
    @SerializedName("canopy_coverage_pct") val canopyCoveragePct: Double?,
    @SerializedName("tree_health") val treeHealth: TreeHealthDto?,
    val observations: List<String>?,
    val recommendations: List<String>?,
    @SerializedName("original_image_url") val originalImageUrl: String?,
    @SerializedName("overlay_image_url") val overlayImageUrl: String?
)

data class TreeHealthDto(
    val healthy: Int?,
    val needs_care: Int?,
    val needs_replacement: Int?
)

