package co.farmpulse.app.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.domain.model.TreeAnalysisResult
import co.farmpulse.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onNavigateToResult: (TreeAnalysisResult) -> Unit, onNewScan: () -> Unit) {
    val history by viewModel.history.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Scan history",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceCharcoal
            )
        )
        
        Text(
            text = "${history.size} analyses total", // Simplified for now
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = SecondaryText
            ),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (history.isEmpty()) {
            EmptyHistory(onNewScan)
        } else {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(0.5.dp, BorderGrey, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(14.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(horizontal = 14.dp)) {
                    itemsIndexed(history) { index, item ->
                        HistoryItem(item, onClick = { onNavigateToResult(item) })
                        if (index < history.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = BorderGrey)
                        }
                    }
                }
            }
            
            QuotaFooter(remaining = 3, onNewScan = onNewScan) // Remaining hardcoded for now
        }
    }
}

@Composable
fun HistoryItem(item: TreeAnalysisResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFC8E6D4), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Park,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = "Farm Analysis", // Name would come from result if available
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceCharcoal
            )
            Text(
                text = "${item.totalTreeCount} trees · Jun 4", // Date placeholder
                fontSize = 11.sp,
                color = SecondaryText
            )
        }
        
        HealthBadge(item)
    }
}

@Composable
fun HealthBadge(result: TreeAnalysisResult) {
    // Simplified logic for demo based on confidence/total as placeholder
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
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun QuotaFooter(remaining: Int, onNewScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$remaining analyses remaining this month",
            fontSize = 11.sp,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onNewScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, ForestGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen)
        ) {
            Text("New scan", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun EmptyHistory(onNewScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Park,
            contentDescription = null,
            tint = BorderGrey,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No scans yet. Upload your first farm photo.",
            fontSize = 13.sp,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNewScan,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            Text("Start first scan", color = Color.White)
        }
    }
}
