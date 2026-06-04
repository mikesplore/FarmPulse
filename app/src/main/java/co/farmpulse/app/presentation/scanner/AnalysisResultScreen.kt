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
import co.farmpulse.app.presentation.home.SectionHeading
import co.farmpulse.app.ui.theme.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    result: TreeAnalysisResult,
    onBack: () -> Unit
) {
    // Note: For this demo, we'll use hardcoded recommendations if they aren't in the model yet.
    // In a real app, these come from the API response (TreeAnalysisResponse).
    val recommendations = listOf(
        "Apply nitrogen-rich fertilizer to the 'Needs Care' sector." to false,
        "Urgent: Prune dead branches in the North-East quadrant before the expected rain on Wednesday." to true,
        "Monitor canopy coverage in newly planted areas; density is 12% below target." to false
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
    ) {
        TopAppBar(
            title = { Text("Analysis result", fontSize = 17.sp, fontWeight = FontWeight.Medium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = ForestGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundOffWhite)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // OverlayImageCard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFDFF0E8)),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder until we have a real URL
                Icon(Icons.Outlined.Park, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(48.dp))
                
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "AI OVERLAY · ${result.totalTreeCount} TREES",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ForestGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // HealthStatsRow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HealthStatCard(
                    modifier = Modifier.weight(1f),
                    count = (result.totalTreeCount * 0.7).toInt(), // Placeholder distribution
                    label = "Healthy",
                    valueColor = ForestGreen,
                    labelColor = Color(0xFF3A8060),
                    bgColor = Color(0xFFEAF4EE)
                )
                HealthStatCard(
                    modifier = Modifier.weight(1f),
                    count = (result.totalTreeCount * 0.2).toInt(),
                    label = "Needs care",
                    valueColor = Color(0xFFB87A12),
                    labelColor = Color(0xFFC88A18),
                    bgColor = Color(0xFFFEF3E0)
                )
                HealthStatCard(
                    modifier = Modifier.weight(1f),
                    count = (result.totalTreeCount * 0.1).toInt(),
                    label = "Replace",
                    valueColor = DangerTerracotta,
                    labelColor = DangerTerracotta,
                    bgColor = Color(0xFFFDE8E3)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // MetaStatsRow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    modifier = Modifier.weight(1f),
                    value = "64%",
                    label = "Canopy cover"
                )
                StatChip(
                    modifier = Modifier.weight(1f),
                    value = "${(result.confidenceScore * 100).toInt()}%",
                    label = "Confidence"
                )
            }

            SectionHeading("Recommendations")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, BorderGrey, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    recommendations.forEachIndexed { index, rec ->
                        RecommendationItem(text = rec.first, isUrgent = rec.second)
                        if (index < recommendations.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = BorderGrey)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HealthStatCard(modifier: Modifier, count: Int, label: String, valueColor: Color, labelColor: Color, bgColor: Color) {
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = count.toString(), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = valueColor)
        Text(text = label, fontSize = 10.sp, color = labelColor)
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
fun RecommendationItem(text: String, isUrgent: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .background(if (isUrgent) Color(0xFFD4850A) else Color(0xFF52B788), CircleShape)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF3A3A35),
            lineHeight = 18.sp
        )
    }
}
