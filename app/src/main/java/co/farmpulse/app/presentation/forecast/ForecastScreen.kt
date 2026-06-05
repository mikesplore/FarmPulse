package co.farmpulse.app.presentation.forecast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.data.remote.dto.DailyForecastDto
import co.farmpulse.app.presentation.components.*
import co.farmpulse.app.ui.theme.*
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(viewModel: ForecastViewModel) {
    val state by viewModel.uiState.collectAsState()

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.loadForecast() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundOffWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Hero Section (Integrated Image + Title + Location) ───────────
                ForecastHero(
                    location = state.locationLabel.ifBlank { "Locating..." },
                    title = "7-day forecast"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Weekly Highlights ─────────────────────────────────────────
                    if (state.daily.isNotEmpty()) {
                        ForecastSummaryCard(state.daily)
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // ── AI Insight section (Forecast) ─────────────────────────────
                    if (state.aiEnabled) {
                        AiInsightSection(
                            summary = state.aiSummary,
                            isLoading = state.isLoading && state.aiSummary == null,
                            onGetInsight = { viewModel.loadForecast() }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // ── Main Forecast Card ────────────────────────────────────────
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, BorderGrey, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            if (state.isLoading && state.daily.isEmpty()) {
                                repeat(7) { ShimmerForecastRow() }
                            } else {
                                state.daily.forEachIndexed { index, day ->
                                    DayRow(day = day, isToday = index == 0)
                                    if (index < state.daily.size - 1) {
                                        HorizontalDivider(thickness = 0.5.dp, color = BorderGrey)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // ── Rain Probability Section ──────────────────────────────────
                    SectionHeading("Rain probability")
                    RainProbabilityChart(daily = state.daily, isLoading = state.isLoading && state.daily.isEmpty())

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Initial Loading Overlay (only for first load)
            AnimatedVisibility(
                visible = state.isLoading && state.daily.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LoadingOverlay("Synchronizing forecast...")
            }
        }
    }
}

@Composable
private fun ForecastHero(location: String, title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        // High-quality farm/landscape hero image
        AsyncImage(
            model = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?q=80&w=1000&auto=format&fit=crop",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay for contrast and depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = location,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ForecastSummaryCard(daily: List<DailyForecastDto>) {
    val rainiestDay = daily.maxByOrNull { it.precipitationProbability ?: 0.0 }
    val warmestDay = daily.maxByOrNull { it.tempMax ?: 0.0 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryHighlight(
            modifier = Modifier.weight(1f),
            label = "Rainiest",
            value = "${rainiestDay?.precipitationProbability?.toInt()}%",
            dateStr = rainiestDay?.date,
            icon = Icons.Outlined.Thunderstorm,
            color = AccentAmber
        )
        SummaryHighlight(
            modifier = Modifier.weight(1f),
            label = "Warmest",
            value = "${warmestDay?.tempMax?.toInt()}°",
            dateStr = warmestDay?.date,
            icon = Icons.Outlined.WbSunny,
            color = ForestGreen
        )
    }
}

@Composable
private fun SummaryHighlight(modifier: Modifier, label: String, value: String, dateStr: String?, icon: ImageVector, color: Color) {
    // Show full day name (e.g., "Monday")
    val dayName = try {
        val date = LocalDate.parse(dateStr)
        date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    } catch (e: Exception) { "Unknown" }

    Card(
        modifier = modifier.border(0.5.dp, BorderGrey, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryText)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = OnSurfaceCharcoal)
            Text(dayName, fontSize = 12.sp, color = SecondaryText)
        }
    }
}

@Composable
private fun DayRow(day: DailyForecastDto, isToday: Boolean) {
    val dayName = try {
        val date = LocalDate.parse(day.date)
        if (isToday) "Today" else date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    } catch (e: Exception) { day.date ?: "--" }

    val isRainy = (day.precipitationProbability ?: 0.0) >= 60.0

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayName,
            modifier = Modifier.width(90.dp), // Accommodates full names like Wednesday
            fontSize = 16.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isToday) ForestGreen else OnSurfaceCharcoal
        )

        Icon(
            imageVector = getIconForCondition(day.conditionCode),
            contentDescription = null,
            modifier = Modifier.weight(1f).size(22.dp),
            tint = if (isRainy) AccentAmber else SecondaryText
        )

        Box(
            modifier = Modifier.weight(1.5f).padding(horizontal = 16.dp).height(6.dp).background(SurfaceVariant, RoundedCornerShape(3.dp))
        ) {
            val fillFraction = (day.precipitationProbability?.toFloat() ?: 0f) / 100f
            Box(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(fillFraction).background(if (isRainy) AccentAmber else LightGreen, RoundedCornerShape(3.dp))
            )
        }

        Row(modifier = Modifier.width(64.dp), horizontalArrangement = Arrangement.End) {
            Text(text = "${day.tempMax?.toInt()}°", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurfaceCharcoal)
            Text(text = " / ${day.tempMin?.toInt()}°", fontSize = 15.sp, color = SecondaryText)
        }
    }
}

@Composable
private fun RainProbabilityChart(daily: List<DailyForecastDto>, isLoading: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().border(0.5.dp, BorderGrey, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Vertical axis labels (Y-axis legend)
                Column(
                    modifier = Modifier.height(120.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("100%", fontSize = 9.sp, color = SecondaryText)
                    Text("50%", fontSize = 9.sp, color = SecondaryText)
                    Text("0%", fontSize = 9.sp, color = SecondaryText)
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        ShimmerBox(Modifier.fillMaxWidth().height(120.dp))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            daily.take(7).forEach { day ->
                                val prob = (day.precipitationProbability ?: 0.0).coerceIn(0.0, 100.0)
                                val barHeight = (120 * prob / 100).dp.coerceAtLeast(6.dp)
                                val barColor = when {
                                    prob >= 60.0 -> AccentAmber
                                    prob < 40.0 -> LightGreen
                                    else -> Color(0xFFB8DAC5)
                                }
                                Box(
                                    modifier = Modifier.weight(1f).height(barHeight).background(barColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            daily.take(7).forEach { day ->
                                val dayShortName = try { 
                                    LocalDate.parse(day.date).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) 
                                } catch (e: Exception) { "?" }
                                Text(text = dayShortName, modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryText, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            // Probability Legend
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = LightGreen, label = "Low risk")
                LegendItem(color = Color(0xFFB8DAC5), label = "Moderate")
                LegendItem(color = AccentAmber, label = "High risk")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, color = SecondaryText)
    }
}

@Composable
private fun ShimmerForecastRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        ShimmerBox(Modifier.width(48.dp).height(20.dp))
        Spacer(modifier = Modifier.width(24.dp))
        ShimmerBox(Modifier.size(24.dp))
        Spacer(modifier = Modifier.weight(1f))
        ShimmerBox(Modifier.width(70.dp).height(20.dp))
    }
}
