package co.farmpulse.app.presentation.forecast

import android.Manifest
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
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ForecastScreen(viewModel: ForecastViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission state for location
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    // Show snackbar if API Key is missing
    LaunchedEffect(state.isApiKeyMissing) {
        if (state.isApiKeyMissing) {
            snackbarHostState.showSnackbar(
                message = "Please add your API key in the settings page",
                duration = SnackbarDuration.Long
            )
        }
    }

    // Trigger update only if we don't have data
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (state.daily.isEmpty()) {
            viewModel.loadForecast(usePreciseLocation = locationPermissionsState.allPermissionsGranted)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { 
                viewModel.loadForecast(
                    usePreciseLocation = locationPermissionsState.allPermissionsGranted,
                    isPullToRefresh = true
                ) 
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
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
                        location = state.locationLabel.ifBlank { "Pinpointing your fields..." },
                        title = "7-Day Farm Outlook"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        // Location Permission Banner
                        if (!locationPermissionsState.allPermissionsGranted) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LocationPermissionBanner {
                                locationPermissionsState.launchMultiplePermissionRequest()
                            }
                        }

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
                                isLoading = state.isRefreshing && state.aiSummary == null,
                                onGetInsight = { 
                                    viewModel.loadForecast(
                                        usePreciseLocation = locationPermissionsState.allPermissionsGranted,
                                        isPullToRefresh = true
                                    ) 
                                }
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
                    LoadingOverlay("Harvesting field data...")
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionBanner(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.MyLocation, null, tint = ForestGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Enable Precise Location",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceCharcoal
                )
                Text(
                    "Get hyper-local forecasts for your exact farm site.",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
            TextButton(onClick = onRequestPermission) {
                Text("Enable", color = ForestGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ForecastHero(location: String, title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?q=80&w=1000&auto=format&fit=crop",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                color = ForestGreen.copy(alpha = 0.9f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "FIELD INTELLIGENCE",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.2.sp
                )
            }

            Text(
                text = title,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1.5).sp,
                lineHeight = 38.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = LightGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = location,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
private fun ShimmerForecastRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(modifier = Modifier.width(90.dp).height(20.dp))
        Spacer(modifier = Modifier.weight(1f))
        ShimmerBox(modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.weight(1.5f).padding(horizontal = 16.dp))
        ShimmerBox(modifier = Modifier.width(64.dp).height(20.dp))
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

                // Bars
                Row(
                    modifier = Modifier.weight(1f).height(120.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (isLoading) {
                        repeat(7) {
                            Box(modifier = Modifier.width(24.dp).fillMaxHeight(0.3f).background(SurfaceVariant, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        }
                    } else {
                        daily.forEach { day ->
                            val prob = (day.precipitationProbability ?: 0.0).toFloat() / 100f
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(prob.coerceAtLeast(0.05f))
                                    .background(
                                        if (prob >= 0.6f) AccentAmber else LightGreen,
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // X-axis labels (Days)
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daily.forEach { day ->
                    val label = try {
                        val date = LocalDate.parse(day.date)
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    } catch (e: Exception) { "--" }
                    Text(label, fontSize = 10.sp, color = SecondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                }
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
            value = "${rainiestDay?.precipitationProbability?.toInt() ?: 0}%",
            dateStr = rainiestDay?.date,
            icon = Icons.Outlined.Thunderstorm,
            color = AccentAmber
        )
        SummaryHighlight(
            modifier = Modifier.weight(1f),
            label = "Warmest",
            value = "${warmestDay?.tempMax?.toInt() ?: 0}°",
            dateStr = warmestDay?.date,
            icon = Icons.Outlined.WbSunny,
            color = ForestGreen
        )
    }
}

@Composable
private fun SummaryHighlight(modifier: Modifier, label: String, value: String, dateStr: String?, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier.border(0.5.dp, BorderGrey, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 11.sp, color = SecondaryText, fontWeight = FontWeight.Bold)
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = OnSurfaceCharcoal)
            }
        }
    }
}
