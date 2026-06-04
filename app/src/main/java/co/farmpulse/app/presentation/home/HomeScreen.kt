package co.farmpulse.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.data.remote.dto.HourlyForecastDto
import co.farmpulse.app.presentation.components.OfflineBanner
import co.farmpulse.app.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .verticalScroll(rememberScrollState())
    ) {
        // Only show OfflineBanner if data is from cache
        if (state.isFromCache && state.cachedAt != null) {
            OfflineBanner(cachedAt = state.cachedAt!!)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // TempHero (Center-aligned)
            TempHero(
                city = state.city,
                region = state.region,
                temp = state.current?.temperature,
                condition = state.current?.conditionCode // This would ideally map to a string like "Partly Cloudy"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // StatRow (3 equal chips)
            StatRow(
                humidity = state.hourly.firstOrNull()?.humidity?.toInt(),
                windSpeed = state.current?.windSpeed,
                feelsLike = state.hourly.firstOrNull()?.feelsLike
            )

            Spacer(modifier = Modifier.height(10.dp))

            // AI Insight Card
            AiInsightSection(
                summary = state.aiSummary,
                isLoading = state.isLoadingAiSummary,
                onGetInsight = { viewModel.loadAiSummary() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Next 6 hours Section
            SectionHeading("Next 6 hours")
            
            val next6Hours = state.hourly.take(6)
            HourlyStrip(next6Hours)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TempHero(city: String, region: String, temp: Double?, condition: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = SecondaryText
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (region.isNotBlank()) "$city, $region" else city,
                fontSize = 13.sp,
                color = SecondaryText
            )
        }
        
        Text(
            text = "${temp?.toInt() ?: "--"}°",
            fontSize = 72.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceCharcoal
        )
        
        Text(
            text = "Partly cloudy", // Placeholder for actual condition text mapping
            fontSize = 15.sp,
            color = SecondaryText
        )
    }
}

@Composable
fun StatRow(humidity: Int?, windSpeed: Double?, feelsLike: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(modifier = Modifier.weight(1f), value = "${humidity ?: "--"}%", label = "Humidity")
        StatChip(modifier = Modifier.weight(1f), value = "${windSpeed?.toInt() ?: "--"} km/h", label = "Wind")
        StatChip(modifier = Modifier.weight(1f), value = "${feelsLike?.toInt() ?: "--"}°", label = "Feels like")
    }
}

@Composable
fun StatChip(modifier: Modifier, value: String, label: String) {
    Column(
        modifier = modifier
            .background(SurfaceVariant, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OnSurfaceCharcoal)
        Text(text = label, fontSize = 11.sp, color = SecondaryText)
    }
}

@Composable
fun AiInsightSection(summary: String?, isLoading: Boolean, onGetInsight: () -> Unit) {
    if (summary == null && !isLoading) {
        OutlinedButton(
            onClick = onGetInsight,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, ForestGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
        ) {
            Text("Get AI insight →", fontWeight = FontWeight.Medium)
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFB8DAC5), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4EE)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(11.dp), tint = ForestGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI INSIGHT", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = ForestGreen, letterSpacing = 0.06.sp)
                }
                Spacer(modifier = Modifier.height(5.dp))
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ForestGreen, trackColor = Color(0xFFB8DAC5).copy(alpha = 0.3f))
                } else {
                    Text(text = summary ?: "", fontSize = 13.sp, color = ForestGreen, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
fun SectionHeading(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = SecondaryText,
            letterSpacing = 0.8.sp
        ),
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
    )
}

@Composable
fun HourlyStrip(hours: List<HourlyForecastDto>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        itemsIndexed(hours) { index, hour ->
            HourlyCell(hour, isActive = index == 1) // Using index 1 as "Active" to match mockup visual
        }
    }
}

@Composable
fun HourlyCell(hour: HourlyForecastDto, isActive: Boolean) {
    val time = try {
        val dt = LocalDateTime.parse(hour.time)
        dt.format(DateTimeFormatter.ofPattern("ha"))
    } catch (e: Exception) {
        "12PM"
    }

    val isRainy = (hour.precipitationProbability ?: 0.0) >= 60.0

    Column(
        modifier = Modifier
            .width(52.dp)
            .background(if (isActive) ForestGreen else SurfaceVariant, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (isActive && hour.time == null) "Now" else time, fontSize = 10.sp, color = if (isActive) Color(0xFFD4F2E1) else SecondaryText)
        Icon(
            imageVector = getIconForCondition(hour.conditionCode),
            contentDescription = null,
            modifier = Modifier.padding(vertical = 4.dp).size(16.dp),
            tint = if (isActive) Color.White else if (isRainy) AccentAmber else ForestGreen
        )
        Text(text = "${hour.temperature?.toInt() ?: "--"}°", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isActive) Color.White else OnSurfaceCharcoal)
    }
}

fun getIconForCondition(code: String?): ImageVector {
    return when (code) {
        "0", "1" -> Icons.Outlined.WbSunny
        "2", "3" -> Icons.Outlined.Cloud
        "51", "53", "55", "80", "81" -> Icons.Outlined.Umbrella // Using Umbrella for rain
        else -> Icons.Outlined.WbCloudy
    }
}
