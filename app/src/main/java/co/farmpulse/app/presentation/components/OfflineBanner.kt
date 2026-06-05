package co.farmpulse.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
fun OfflineBanner(isOffline: Boolean, cachedAt: Long? = null) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val timeAgo = remember(cachedAt) { cachedAt?.let { getTimeAgo(it) } }
        val message = if (timeAgo != null) "Offline — last updated $timeAgo" else "Offline — no internet connection"

        Surface(color = AccentAmber.copy(alpha = 0.12f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Outlined.WifiOff,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = AccentAmber
                )
            }
        }
    }
}

private fun getTimeAgo(cachedAt: Long): String {
    val diff = System.currentTimeMillis() - cachedAt
    if (diff < 0) return "just now"
    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    
    return when {
        days > 0 -> "$days day${if (days == 1L) "" else "s"} ago"
        hours > 0 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        mins > 0 -> "$mins minute${if (mins == 1L) "" else "s"} ago"
        else -> "just now"
    }
}
