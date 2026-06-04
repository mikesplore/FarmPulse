package co.farmpulse.app.presentation.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.data.remote.dto.DailyForecastDto
import co.farmpulse.app.presentation.home.SectionHeading
import co.farmpulse.app.presentation.home.getIconForCondition
import co.farmpulse.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun ForecastScreen(viewModel: ForecastViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        
        Text(
            text = "7-day forecast",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceCharcoal
            )
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = SecondaryText
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Nairobi, Kenya", // Placeholder for now, should come from state
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, BorderGrey, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                state.daily.forEachIndexed { index, day ->
                    DayRow(day, index == 0)
                    if (index < state.daily.size - 1) {
                        HorizontalDivider(thickness = 0.5.dp, color = BorderGrey)
                    }
                }
            }
        }

        SectionHeading("Rain probability")
        
        RainProbabilityChart(state.daily)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DayRow(day: DailyForecastDto, isToday: Boolean) {
    val dateLabel = try {
        val date = LocalDate.parse(day.date)
        if (isToday) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } catch (e: Exception) {
        day.date ?: "--"
    }

    val isRainy = (day.precipitationProbability ?: 0.0) >= 60.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            modifier = Modifier.width(42.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = if (isToday) ForestGreen else OnSurfaceCharcoal,
                fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal
            )
        )
        
        Icon(
            imageVector = getIconForCondition(day.conditionCode),
            contentDescription = null,
            modifier = Modifier
                .weight(1f)
                .size(16.dp),
            tint = if (isRainy) AccentAmber else SecondaryText
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(4.dp)
                .background(BorderGrey, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((day.precipitationProbability?.toFloat() ?: 0f) / 100f)
                    .background(
                        if (isRainy) AccentAmber else LightGreen,
                        RoundedCornerShape(2.dp)
                    )
            )
        }

        Row(
            modifier = Modifier.width(50.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${day.tempMax?.toInt() ?: "--"}°",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceCharcoal
                )
            )
            Text(
                text = " / ${day.tempMin?.toInt() ?: "--"}°",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            )
        }
    }
}

@Composable
fun RainProbabilityChart(daily: List<DailyForecastDto>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(0.5.dp, BorderGrey, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            daily.take(7).forEach { day ->
                val prob = day.precipitationProbability ?: 0.0
                val barColor = if (prob >= 60.0) AccentAmber else if (prob < 40.0) LightGreen else Color(0xFFB8DAC5)
                val dayName = try {
                    LocalDate.parse(day.date).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                } catch (e: Exception) {
                    "?"
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((46 * (prob / 100)).dp.coerceAtLeast(4.dp))
                            .background(barColor, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = if (prob >= 60.0) AccentAmber else SecondaryText,
                            fontWeight = if (prob >= 60.0) FontWeight.Medium else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}
