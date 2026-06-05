package co.farmpulse.app.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.domain.model.TreeAnalysisResult
import co.farmpulse.app.ui.theme.*
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToResult: (TreeAnalysisResult) -> Unit,
    onNewScan: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    
    // Group items by date for a better UX
    val groupedItems = remember(state.items) {
        state.items.groupBy { item ->
            val date = Instant.ofEpochMilli(item.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            date
        }.toSortedMap(compareByDescending { it })
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.refreshHistory() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundOffWhite)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Scan history",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceCharcoal,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Text(
                text = "${state.items.size} analyses total",
                fontSize = 13.sp,
                color = SecondaryText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            if (state.items.isEmpty() && !state.isLoading) {
                EmptyHistory(onNewScan)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedItems.forEach { (date, items) ->
                        item {
                            DateHeader(date)
                        }
                        items(items) { item ->
                            HistoryCard(item, onClick = { onNavigateToResult(item) })
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        QuotaFooter(remaining = 3, onNewScan = onNewScan)
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }
    val today = LocalDate.now()
    val label = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(formatter)
    }
    
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = SecondaryText,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun HistoryCard(item: TreeAnalysisResult, onClick: () -> Unit) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeStr = remember(item.timestamp) { timeFormatter.format(Date(item.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, BorderGrey, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Icon
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEAF4EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Park,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = if (!item.county.isNullOrBlank()) "Farm in ${item.county}" else "Farm Analysis",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceCharcoal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.totalTreeCount} trees detected · $timeStr",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                HealthBadge(item)
            }
            
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = BorderGrey,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HealthBadge(result: TreeAnalysisResult) {
    val status = when {
        result.confidenceScore >= 0.8 -> "Healthy"
        result.confidenceScore >= 0.5 -> "Needs care"
        else -> "Critical"
    }
    
    val (bgColor, textColor) = when (status) {
        "Healthy" -> Color(0xFFEAF4EE) to ForestGreen
        "Needs care" -> Color(0xFFFEF3E0) to Color(0xFFB87A12)
        else -> Color(0xFFFDE8E3) to DangerTerracotta
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun QuotaFooter(remaining: Int, onNewScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$remaining analyses remaining this month",
            fontSize = 12.sp,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNewScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            Text("Start new scan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun EmptyHistory(onNewScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SurfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Park,
                contentDescription = null,
                tint = BorderGrey,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No scans yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceCharcoal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Upload your first farm photo to see AI-powered tree analysis here.",
            fontSize = 14.sp,
            color = SecondaryText,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNewScan,
            modifier = Modifier.height(52.dp).fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            Text("Start first scan", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
