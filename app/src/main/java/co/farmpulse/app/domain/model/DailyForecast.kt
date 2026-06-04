package co.farmpulse.app.domain.model

data class DailyForecast(
    val date: String = "",
    val tempMin: Double = 0.0,
    val tempMax: Double = 0.0,
    val conditionCode: String = ""
)

