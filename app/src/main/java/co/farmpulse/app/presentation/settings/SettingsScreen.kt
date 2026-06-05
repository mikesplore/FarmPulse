package co.farmpulse.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.farmpulse.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    PullToRefreshBox(
        isRefreshing = state.isLoadingUsage,
        onRefresh = { viewModel.loadUsage() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundOffWhite)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Screen title — Updated to 22.sp Bold for consistency across major screens
            Text(
                text = "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceCharcoal,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // ── API Usage ────────────────────────────────────────────────────────
            SettingsSectionLabel("API usage this month")
            UsageCard(state = state)

            Spacer(modifier = Modifier.height(24.dp))

            // ── AI & Language ────────────────────────────────────────────────────
            SettingsSectionLabel("AI & language")
            SettingsGroup {
                // AI summaries toggle
                SwitchRow(
                    label    = "AI weather summaries",
                    subLabel = "Insight cards on Home & Forecast",
                    checked  = state.aiEnabled,
                    onToggle = { viewModel.setAiEnabled(it) },
                    icon     = Icons.Outlined.AutoAwesome
                )
                Divider()
                // Language selector — EN / SW
                SelectRow(
                    label   = "Summary language",
                    options = listOf("English" to "en", "Kiswahili" to "sw"),
                    current = state.lang,
                    onSelect = { viewModel.setLang(it) },
                    icon    = Icons.Outlined.Translate
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Units ────────────────────────────────────────────────────────────
            SettingsSectionLabel("Units")
            SettingsGroup {
                SelectRow(
                    label   = "Temperature",
                    options = listOf("Metric (°C)" to "metric", "Imperial (°F)" to "imperial"),
                    current = state.units,
                    onSelect = { viewModel.setUnits(it) },
                    icon    = Icons.Outlined.Thermostat
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Location ─────────────────────────────────────────────────────────
            SettingsSectionLabel("Location")
            SettingsGroup {
                InfoRow(
                    label    = "Auto-detected city",
                    value    = state.ipCity ?: "Detecting…",
                    subLabel = "From IP — may be inaccurate",
                    icon     = Icons.Outlined.MyLocation
                )
                Divider()
                LocationOverrideRow(
                    cityOverride = state.cityOverride,
                    latOverride  = state.latOverride,
                    lonOverride  = state.lonOverride,
                    onCityChange = { viewModel.setCityOverride(it) },
                    onLatChange  = { viewModel.setLatOverride(it) },
                    onLonChange  = { viewModel.setLonOverride(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Plan info ────────────────────────────────────────────────────────
            SettingsSectionLabel("Plan")
            SettingsGroup {
                InfoRow(
                    label = "Current plan",
                    value = state.planName.replaceFirstChar { it.uppercase() },
                    icon  = Icons.Outlined.Verified
                )
                Divider()
                InfoRow(
                    label    = "Forecast days",
                    value    = "${state.maxDays} days",
                    subLabel = "Maximum for your plan",
                    icon     = Icons.Outlined.CalendarToday
                )
                Divider()
                InfoRow(
                    label    = "Period resets",
                    value    = state.periodEnd ?: "—",
                    icon     = Icons.Outlined.Refresh
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun UsageCard(state: SettingsUiState) {
    // If not refreshing (isLoadingUsage managed by PullToRefreshBox), show content
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(0.5.dp, BorderGrey, RoundedCornerShape(14.dp))
            .background(SurfaceWhite, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        // Requests
        UsageBar(
            label    = "API requests",
            used     = state.requestsUsed,
            limit    = state.requestsLimit,
            remaining = state.requestsRemaining
        )
        Spacer(modifier = Modifier.height(14.dp))
        // AI requests
        UsageBar(
            label    = "AI requests",
            used     = state.aiRequestsUsed,
            limit    = state.aiRequestsLimit,
            remaining = state.aiRequestsRemaining
        )
    }
}

@Composable
private fun UsageBar(label: String, used: Int, limit: Int, remaining: Int) {
    val fraction = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val isNearLimit = fraction >= 0.8f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = SecondaryText)
        Text(
            text = "$used / $limit",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isNearLimit) AccentAmber else ForestGreen
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(SurfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isNearLimit) AccentAmber else LightGreen)
        )
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text("$remaining remaining", fontSize = 11.sp, color = SecondaryText)
}

@Composable
private fun LocationOverrideRow(
    cityOverride: String,
    latOverride:  String,
    lonOverride:  String,
    onCityChange: (String) -> Unit,
    onLatChange:  (String) -> Unit,
    onLonChange:  (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.EditLocation, null, modifier = Modifier.size(18.dp), tint = SecondaryText)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Override location", fontSize = 13.sp, color = OnSurfaceCharcoal, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value         = cityOverride,
            onValueChange = onCityChange,
            modifier      = Modifier.fillMaxWidth(),
            label         = { Text("City name", fontSize = 12.sp) },
            placeholder   = { Text("e.g. Mombasa", fontSize = 12.sp, color = SecondaryText) },
            singleLine    = true,
            shape         = RoundedCornerShape(8.dp),
            colors        = settingsTextFieldColors()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value         = latOverride,
                onValueChange = onLatChange,
                modifier      = Modifier.weight(1f),
                label         = { Text("Latitude", fontSize = 12.sp) },
                placeholder   = { Text("-4.05", fontSize = 12.sp, color = SecondaryText) },
                singleLine    = true,
                shape         = RoundedCornerShape(8.dp),
                colors        = settingsTextFieldColors()
            )
            OutlinedTextField(
                value         = lonOverride,
                onValueChange = onLonChange,
                modifier      = Modifier.weight(1f),
                label         = { Text("Longitude", fontSize = 12.sp) },
                placeholder   = { Text("39.66", fontSize = 12.sp, color = SecondaryText) },
                singleLine    = true,
                shape         = RoundedCornerShape(8.dp),
                colors        = settingsTextFieldColors()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Overrides GPS/IP detection for all weather requests.",
            fontSize = 11.sp,
            color = SecondaryText
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = SecondaryText,
        letterSpacing = 0.07.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(0.5.dp, BorderGrey, RoundedCornerShape(14.dp))
            .background(SurfaceWhite, RoundedCornerShape(14.dp)),
        content = content
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(thickness = 0.5.dp, color = BorderGrey)
}

@Composable
private fun SwitchRow(
    label: String,
    subLabel: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = SecondaryText)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = OnSurfaceCharcoal)
            subLabel?.let {
                Text(it, fontSize = 11.sp, color = SecondaryText)
            }
        }
        Switch(
            checked  = checked,
            onCheckedChange = onToggle,
            colors   = SwitchDefaults.colors(
                checkedThumbColor  = SurfaceWhite,
                checkedTrackColor  = ForestGreen,
                uncheckedThumbColor = SurfaceWhite,
                uncheckedTrackColor = SurfaceVariant,
                uncheckedBorderColor = BorderGrey
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun SelectRow(
    label: String,
    options: List<Pair<String, String>>,  // display to value
    current: String,
    onSelect: (String) -> Unit,
    icon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.second == current }?.first ?: current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = SecondaryText)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = OnSurfaceCharcoal, modifier = Modifier.weight(1f))
        Box {
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(currentLabel, fontSize = 13.sp, color = ForestGreen, fontWeight = FontWeight.Medium)
                Icon(Icons.Outlined.ExpandMore, null, modifier = Modifier.size(16.dp), tint = ForestGreen)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (displayName, value) ->
                    DropdownMenuItem(
                        text    = { Text(displayName, fontSize = 13.sp) },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        },
                        leadingIcon = if (value == current) ({
                            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp), tint = ForestGreen)
                        }) else null
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    subLabel: String? = null,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = SecondaryText)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = OnSurfaceCharcoal)
            subLabel?.let { Text(it, fontSize = 11.sp, color = SecondaryText) }
        }
        Text(value, fontSize = 13.sp, color = SecondaryText)
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor  = SurfaceVariant,
    focusedContainerColor    = SurfaceVariant,
    unfocusedBorderColor     = BorderGrey,
    focusedBorderColor       = ForestGreen,
    cursorColor              = ForestGreen,
    unfocusedLabelColor      = SecondaryText,
    focusedLabelColor        = ForestGreen
)
