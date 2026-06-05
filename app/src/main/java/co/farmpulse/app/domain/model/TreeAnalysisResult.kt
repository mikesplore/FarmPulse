package co.farmpulse.app.domain.model

data class TreeAnalysisResult(
    val analysisId: String = "",
    val totalTreeCount: Int = 0,
    val confidenceScore: Double = 0.0,
    val timestamp: Long = 0L,
    val county: String? = null,
    val imageUrl: String? = null
)
