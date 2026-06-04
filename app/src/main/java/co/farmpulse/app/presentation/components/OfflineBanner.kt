package co.farmpulse.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.ui.theme.AccentAmber
import java.util.concurrent.TimeUnit

@Composable
fun OfflineBanner(cachedAt: Long) {
    val timeAgo = remember(cachedAt) { getTimeAgo(cachedAt) }
    Surface(color = AccentAmber.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = AccentAmber,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Offline — last updated $timeAgo",
                fontSize = 13.sp,
                color = AccentAmber
            )
        }
    }
}

private fun getTimeAgo(cachedAt: Long): String {
    val diff = System.currentTimeMillis() - cachedAt
    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    return when {
        hours > 0 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        mins > 0 -> "$mins minute${if (mins == 1L) "" else "s"} ago"
        else -> "just now"
    }
}
