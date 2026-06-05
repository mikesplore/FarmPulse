package co.farmpulse.app.presentation.home

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.data.remote.dto.HourlyForecastDto
import co.farmpulse.app.presentation.components.*
import co.farmpulse.app.ui.theme.*
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
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

    // Initial load logic - only if we don't have data yet
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (state.current == null) {
            viewModel.loadWeather(usePreciseLocation = locationPermissionsState.allPermissionsGranted)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = {
                viewModel.loadWeather(
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
                    // ── Hero Section with Image ───────────────────────────────────────
                    HomeHero(
                        city = state.city,
                        region = state.region,
                        temp = state.current?.temperature,
                        condition = getConditionText(state.current?.conditionCode),
                        iconUrl = state.current?.icon
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        // Offline banner - reactive to real-time network state
                        OfflineBanner(isOffline = state.isOffline, cachedAt = state.cachedAt)

                        // Permission Request Banner if not granted
                        if (!locationPermissionsState.allPermissionsGranted) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LocationPermissionBanner {
                                locationPermissionsState.launchMultiplePermissionRequest()
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // ── StatRow (Primary Metrics) ─────────────────────────────────
                        StatRow(
                            humidity = state.current?.humidity?.toInt(),
                            windSpeed = state.current?.windSpeed,
                            feelsLike = state.current?.feelsLike
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // ── AI Insight section ────────────────────────────────────────
                        if (state.aiEnabled) {
                            AiInsightSection(
                                summary = state.aiSummary,
                                isLoading = state.isLoadingAiSummary,
                                onGetInsight = { viewModel.refreshWithAi() }
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                        }

                        // ── Next 6 Hours ──────────────────────────────────────────────
                        SectionHeading(text = "Next 6 hours")
                        HourlyStrip(hours = state.hourly.take(6))

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Today's Details Grid ──────────────────────────────────────
                        SectionHeading(text = "Today's details")
                        DetailsGrid(state)

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                // Global Loading Overlay (only for first load)
                AnimatedVisibility(
                    visible = state.isLoading && state.current == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LoadingOverlay("Synchronizing farm data...")
                }
            }
        }
    }
}


@Composable
private fun HourlyStrip(hours: List<HourlyForecastDto>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(hours) { index, hour ->
            HourlyCell(hour = hour, isNow = index == 0)
        }
    }
}

@Composable
private fun HourlyCell(hour: HourlyForecastDto, isNow: Boolean) {
    val timeLabel = if (isNow) "Now" else {
        try { LocalDateTime.parse(hour.time).format(DateTimeFormatter.ofPattern("ha")) } catch (e: Exception) { "--" }
    }
    val bgColor = if (isNow) ForestGreen else SurfaceVariant
    val contentColor = if (isNow) Color.White else OnSurfaceCharcoal
    
    Column(
        modifier = Modifier
            .width(72.dp)
            .background(bgColor, RoundedCornerShape(14.dp))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = timeLabel, fontSize = 11.sp, color = if (isNow) Color(0xFFD4F2E1) else SecondaryText)
        Spacer(modifier = Modifier.height(10.dp))
        
        // Use backend SVG icon for accurate day/night representation
        AsyncImage(
            model = hour.icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            // Use rememberVectorPainter to correctly handle ImageVector as an error fallback
            error = rememberVectorPainter(getIconForCondition(hour.conditionCode)),
            colorFilter = if (isNow) ColorFilter.tint(Color.White) else ColorFilter.tint(ForestGreen)
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "${hour.temperature?.toInt() ?: "--"}°", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = contentColor)
    }
}

@Composable
private fun LocationPermissionBanner(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, LightGreen.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.MyLocation, null, tint = ForestGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Precise Location",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceCharcoal
                )
                Text(
                    "Enable location for hyper-local farm weather.",
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
private fun HomeHero(city: String, region: String, temp: Double?, condition: String?, iconUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1625246333195-78d9c38ad449?q=80&w=1470&auto=format&fit=crop",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (city.isNotBlank()) "$city, $region" else "Locating farm...",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${temp?.toInt() ?: "--"}°",
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    lineHeight = 110.sp
                )
                
                if (!iconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = iconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.9f))
                    )
                }
            }

            Text(
                text = condition ?: "Analyzing sky...",
                fontSize = 22.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StatRow(humidity: Int?, windSpeed: Double?, feelsLike: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatChip(Modifier.weight(1f), "${humidity ?: "--"}%", "Humidity")
        StatChip(Modifier.weight(1f), "${windSpeed?.toInt() ?: "--"} km/h", "Wind")
        StatChip(Modifier.weight(1f), "${feelsLike?.toInt() ?: "--"}°", "Feels like")
    }
}

@Composable
private fun StatChip(modifier: Modifier, value: String, label: String) {
    Column(
        modifier = modifier
            .background(SurfaceVariant, RoundedCornerShape(14.dp))
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurfaceCharcoal)
        Text(text = label, fontSize = 13.sp, color = SecondaryText)
    }
}

@Composable
private fun DetailsGrid(state: HomeUiState) {
    val daily = state.daily.firstOrNull()
    val current = state.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailCard(Modifier.weight(1f), "Sunrise", daily?.sunrise?.takeLast(5) ?: "--", Icons.Outlined.WbTwilight)
            DetailCard(Modifier.weight(1f), "Sunset", daily?.sunset?.takeLast(5) ?: "--", Icons.Outlined.WbTwilight)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailCard(Modifier.weight(1f), "UV Index", "${current?.uvIndex?.toInt() ?: "--"}", Icons.Outlined.WbSunny)
            DetailCard(Modifier.weight(1f), "Rain chance", "${daily?.precipitationProbability?.toInt() ?: "--"}%", Icons.Outlined.Umbrella)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailCard(Modifier.weight(1f), "Wind gust", "${current?.windGust?.toInt() ?: "--"} km/h", Icons.Outlined.Air)
            DetailCard(Modifier.weight(1f), "Precip. sum",
                daily?.precipitationSum?.let { "%.1f mm".format(it) } ?: "--", Icons.Outlined.WaterDrop)
        }
    }
}

@Composable
private fun DetailCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Card(
        modifier = modifier.border(0.5.dp, BorderGrey, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = ForestGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, color = SecondaryText)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurfaceCharcoal)
        }
    }
}
