package co.farmpulse.app.presentation.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.domain.model.TreeAnalysisResult
import co.farmpulse.app.presentation.components.SectionHeading
import co.farmpulse.app.ui.theme.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    result: TreeAnalysisResult,
    onBack: () -> Unit
) {
    // Demo recommendations mapping to expected design pattern
    val recommendations = listOf(
        "Apply nitrogen-rich fertilizer to the 'Needs Care' sector." to false,
        "Urgent: Prune dead branches in the North-East quadrant before the expected rain on Wednesday." to true,
        "Monitor canopy coverage in newly planted areas; density is 12% below target." to false
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Analysis result", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = OnSurfaceCharcoal) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = ForestGreen)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundOffWhite)
                )
                HorizontalDivider(thickness = 0.5.dp, color = BorderGrey)
            }
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── AI Overlay Image ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEAF4EE))
                    .border(1.dp, Color(0xFFB8DAC5), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // In a real app, result.imageUrl would be used here
                Icon(
                    imageVector = Icons.Outlined.Park,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(48.dp)
                )
                
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "AI OVERLAY · ${result.totalTreeCount} TREES",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Health Distribution Row ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HealthStatCard(
                    modifier = Modifier.weight(1f),
                    count = (result.totalTreeCount * 0.7).toInt(),
                    label = "Healthy",
                    valueColor = ForestGreen,
                    bgColor = Color(0xFFEAF4EE)
                )
                HealthStatCard(
                    modifier = Modifier.weight(1f),
                    count = (result.totalTreeCount * 0.2).toInt(),
                    label = "Needs care",
                    valueColor = Color(0xFFB87A12),
                    bgColor = Color(0xFFFEF3E0)
                )
                HealthStatCard(
                    modifier = Modifier.weight(1f),
                    count = (result.totalTreeCount * 0.1).toInt(),
                    label = "Replace",
                    valueColor = DangerTerracotta,
                    bgColor = Color(0xFFFDE8E3)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Key Metrics Row ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ResultStatChip(
                    modifier = Modifier.weight(1f),
                    value = "64%",
                    label = "Canopy cover"
                )
                ResultStatChip(
                    modifier = Modifier.weight(1f),
                    value = "${(result.confidenceScore * 100).toInt()}%",
                    label = "AI Confidence"
                )
            }

            // ── Recommendations Section ───────────────────────────────────────
            SectionHeading("Recommendations")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, BorderGrey, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    recommendations.forEachIndexed { index, rec ->
                        RecommendationItem(text = rec.first, isUrgent = rec.second)
                        if (index < recommendations.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = BorderGrey)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HealthStatCard(modifier: Modifier, count: Int, label: String, valueColor: Color, bgColor: Color) {
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ResultStatChip(modifier: Modifier, value: String, label: String) {
    Column(
        modifier = modifier
            .background(SurfaceVariant, RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurfaceCharcoal)
        Text(text = label, fontSize = 12.sp, color = SecondaryText)
    }
}

@Composable
private fun RecommendationItem(text: String, isUrgent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(if (isUrgent) AccentAmber else LightGreen, CircleShape)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = OnSurfaceCharcoal,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
