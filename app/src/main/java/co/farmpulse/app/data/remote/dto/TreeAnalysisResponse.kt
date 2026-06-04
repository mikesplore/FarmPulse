package co.farmpulse.app.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class TreeAnalysisResponse(
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
    @SerializedName("tree_health") val treeHealth: TreeHealthDto? = null,
    @SerializedName("low_confidence") val lowConfidence: Boolean? = null,
    @SerializedName("tree_species_guess") val treeSpeciesGuess: String? = null,
    @SerializedName("image_perspective") val imagePerspective: String? = null,
    @SerializedName("coverage_estimate") val coverageEstimate: String? = null,
    @SerializedName("cv_count_used") val cvCountUsed: Boolean? = null,
    @SerializedName("observations") val observations: List<String>? = null,
    @SerializedName("recommendations") val recommendations: List<String>? = null,
    @SerializedName("original_image_url") val originalImageUrl: String? = null,
    @SerializedName("overlay_image_url") val overlayImageUrl: String? = null,
    @SerializedName("cv_debug") val cvDebug: JsonElement? = null,
    @SerializedName("gemini_error") val geminiError: String? = null
)

data class TreeHealthDto(
    @SerializedName("healthy") val healthy: Int? = null,
    @SerializedName("needs_care") val needsCare: Int? = null,
    @SerializedName("needs_replacement") val needsReplacement: Int? = null
)

