package co.farmpulse.app.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.ui.theme.*

@Composable
fun AiInsightSection(
    summary: String?,
    isLoading: Boolean,
    onGetInsight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, if (summary != null) Color(0xFFB8DAC5) else BorderGrey, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = if (summary != null) Color(0xFFEAF4EE) else SurfaceWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp), tint = ForestGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI INSIGHT", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ForestGreen, letterSpacing = 0.1.sp)
                }
                if (summary == null && !isLoading) {
                    TextButton(onClick = onGetInsight, contentPadding = PaddingValues(0.dp)) {
                        Text("Get insight →", fontSize = 14.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = ForestGreen,
                    trackColor = Color(0xFFB8DAC5).copy(alpha = 0.3f)
                )
            } else if (summary != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = summary, fontSize = 16.sp, color = ForestGreen, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "No insight available. Try refreshing.", fontSize = 14.sp, color = SecondaryText)
            }
        }
    }
}
