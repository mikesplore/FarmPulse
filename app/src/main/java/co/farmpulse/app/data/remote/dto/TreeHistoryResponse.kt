package co.farmpulse.app.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class TreeHistoryResponse(
    @SerializedName("analyses") val analyses: List<TreeAnalysisHistoryDto>? = null,
    @SerializedName("next_cursor") val nextCursor: String? = null
)

data class TreeAnalysisHistoryDto(
    @SerializedName("analysis_id") val analysisId: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("farmer_id") val farmerId: String? = null,
    @SerializedName("county") val county: String? = null,
    @SerializedName("location") val location: JsonElement? = null,
    @SerializedName("land_acres") val landAcres: Double? = null,
    @SerializedName("total_tree_count") val totalTreeCount: Int? = null,
    @SerializedName("tree_density_per_acre") val treeDensityPerAcre: Double? = null,
    @SerializedName("confidence_score") val confidenceScore: Double? = null,
    @SerializedName("canopy_coverage_pct") val canopyCoveragePct: Double? = null,
    @SerializedName("overlay_image_url") val overlayImageUrl: String? = null,
    @SerializedName("original_image_url") val originalImageUrl: String? = null
)

