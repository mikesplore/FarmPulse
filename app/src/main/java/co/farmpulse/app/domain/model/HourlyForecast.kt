package co.farmpulse.app.domain.model

data class HourlyForecast(
    val time: String = "",
    val temperature: Double = 0.0,
    val precipitationProbability: Int = 0
)

