package co.farmpulse.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.ui.theme.BackgroundOffWhite
import co.farmpulse.app.ui.theme.ForestGreen
import co.farmpulse.app.ui.theme.SecondaryText

@Composable
fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = ForestGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message,
                color = ForestGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val brush = Brush.horizontalGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.1f),
            Color.LightGray.copy(alpha = 0.3f)
        ),
        startX = translateAnim - 300,
        endX = translateAnim
    )
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(brush))
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        color = SecondaryText,
        letterSpacing = 1.5.sp,
        modifier = modifier.padding(top = 32.dp, bottom = 16.dp)
    )
}

fun getIconForCondition(code: String?): ImageVector = when (code) {
    "0", "1" -> Icons.Outlined.WbSunny
    "2", "3" -> Icons.Outlined.Cloud
    "51", "53", "55", "61", "63", "65", "80", "81", "82" -> Icons.Outlined.Umbrella
    "71", "73", "75", "77" -> Icons.Outlined.AcUnit
    "95", "96", "99" -> Icons.Outlined.Thunderstorm
    else -> Icons.Outlined.WbCloudy
}

fun getConditionText(code: String?): String = when (code) {
    "0" -> "Clear sky"
    "1" -> "Mainly clear"
    "2" -> "Partly cloudy"
    "3" -> "Overcast"
    "45", "48" -> "Fog"
    "51", "53", "55" -> "Drizzle"
    "61", "63", "65" -> "Rain"
    "80", "81", "82" -> "Rain showers"
    else -> "Partly cloudy"
}
