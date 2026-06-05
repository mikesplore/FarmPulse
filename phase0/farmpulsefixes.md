# FarmPulse — Bug Fix & Feature Completion Guide

This document covers every issue identified in the review and maps each one to
the exact file(s) that need changing. Follow the order below: data layer first,
then ViewModels, then UI screens.

---

## 1. Root cause: wrong / missing API parameters

All five server endpoints accept the same optional parameters but the app was
calling them with only `lat` and `lon`. The missing parameters matter because:

| Param | Default | Problem when omitted |
|-------|---------|----------------------|
| `ai`  | `true`  | AI summary was being **returned** by the server but the app never read or stored it, so AI Insight always appeared blank |
| `lang` | `en`  | No way to switch to Kiswahili (`sw`) even though the server supports it |
| `units` | `metric` | Should always be sent explicitly to prevent ambiguity |
| `days` | `7` | Send explicitly; value is plan-limited (free = 7 max) |

### Fix in: `WeatherApiService.kt` (Retrofit interface)

Add parameters to **every** endpoint call:

```kotlin
// BEFORE (example)
@GET("v1/weather")
suspend fun getWeather(
    @Query("lat") lat: Double,
    @Query("lon") lon: Double
): WeatherResponse

// AFTER — add to ALL five endpoints (weather, forecast, current, daily, hourly)
@GET("v1/weather")
suspend fun getWeather(
    @Query("lat")   lat:   Double,
    @Query("lon")   lon:   Double,
    @Query("days")  days:  Int     = 7,
    @Query("ai")    ai:    Boolean = true,
    @Query("units") units: String  = "metric",
    @Query("lang")  lang:  String  = "en"
): WeatherResponse

// weather-geo endpoint — also add ip=auto for auto-detection
@GET("v1/weather-geo")
suspend fun getWeatherGeo(
    @Query("ip")    ip:    String  = "auto",
    @Query("days")  days:  Int     = 7,
    @Query("ai")    ai:    Boolean = true,
    @Query("units") units: String  = "metric",
    @Query("lang")  lang:  String  = "en"
): WeatherGeoResponse
```

---

## 2. Data model: `WeatherResponse` / `WeatherGeoResponse` missing fields

### Fix in: `WeatherResponse.kt` (and related DTOs)

#### 2a. Top-level response — add `ai_summary` field

The server returns a top-level `ai_summary` object when `ai=true`. It was
completely absent from the data model.

```kotlin
// ADD to WeatherResponse
data class WeatherResponse(
    val location: LocationDto,
    val current:  CurrentWeatherDto,
    val hourly:   List<HourlyForecastDto>,
    val daily:    List<DailyForecastDto>,
    val aiSummary: AiSummaryDto? = null   // ← ADD THIS (maps to "ai_summary")
)

@JsonClass(generateAdapter = true)
data class AiSummaryDto(
    @Json(name = "summary")   val summary:  String? = null,
    @Json(name = "lang")      val lang:     String? = null,
    @Json(name = "generated_at") val generatedAt: String? = null
)
```

For `weather-geo` also add:

```kotlin
data class WeatherGeoResponse(
    // … existing fields …
    val ipGeo:     IpGeoDto?     = null,  // maps to "ip_geo"
    val aiSummary: AiSummaryDto? = null
)

@JsonClass(generateAdapter = true)
data class IpGeoDto(
    val country: String? = null,
    val region:  String? = null,
    val city:    String? = null,
    val lat:     Double? = null,
    val lon:     Double? = null,
    val org:     String? = null
)
```

#### 2b. `CurrentWeatherDto` — add missing fields

```kotlin
data class CurrentWeatherDto(
    val time:          String? = null,
    val temperature:   Double? = null,
    val windSpeed:     Double? = null,   // "wind_speed"
    val windDirection: Int?    = null,   // "wind_direction"  ← ADD
    val conditionCode: String? = null,
    val icon:          String? = null,
    val humidity:      Double? = null,
    val feelsLike:     Double? = null,   // "feels_like"
    val uvIndex:       Double? = null,   // "uv_index"
    val windGust:      Double? = null    // "wind_gust"  ← ADD
)
```

#### 2c. `HourlyForecastDto` — already has most fields; verify `windGust` present

```kotlin
data class HourlyForecastDto(
    val time:                    String? = null,
    val temperature:             Double? = null,
    val precipitationProbability: Double? = null,
    val windSpeed:               Double? = null,
    val conditionCode:           String? = null,
    val icon:                    String? = null,
    val humidity:                Double? = null,
    val feelsLike:               Double? = null,
    val windGust:                Double? = null,  // ← verify present
    val uvIndex:                 Double? = null,
    val iconPath:                String? = null   // "icon_path"
)
```

#### 2d. `DailyForecastDto` — add missing fields

```kotlin
data class DailyForecastDto(
    val date:                    String? = null,
    val tempMin:                 Double? = null,
    val tempMax:                 Double? = null,
    val precipitationSum:        Double? = null,  // "precipitation_sum" ← ADD
    val sunrise:                 String? = null,
    val sunset:                  String? = null,
    val conditionCode:           String? = null,
    val icon:                    String? = null,
    val precipitationProbability: Double? = null,
    val windMax:                 Double? = null   // "wind_max" ← ADD
)
```

---

## 3. Bug: "Next 6 hours" shows midnight hours instead of current + next 5

### Fix in: `HomeViewModel.kt`

The hourly array from the server always starts at `T00:00` (midnight of the
current day). `state.hourly.take(6)` therefore shows 00:00, 01:00, 02:00 … even
at noon.

```kotlin
// In the function that builds HomeUiState from WeatherResponse:

// BEFORE
val upcomingHours = response.hourly.take(6)

// AFTER — filter to hours >= now, then take 6
val nowHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
val upcomingHours = response.hourly
    .filter { hour ->
        try {
            val t = LocalDateTime.parse(hour.time)
            !t.isBefore(nowHour)
        } catch (e: Exception) { false }
    }
    .take(6)
```

Also update `HourlyCell` in `HomeScreen.kt`:

```kotlin
// The "isNow = index == 0" logic stays correct AFTER the ViewModel fix,
// because index 0 in upcomingHours will now always be the current hour.
// No UI change needed here beyond confirming the fix flows through.
```

---

## 4. Bug: City shows "Nairobi" instead of user's real location

### Root cause

The app was calling `weather-geo` with `ip=auto`. The server's `ip_geo.city`
returns the **IP geolocation city** (Nairobi, because the user is on a Kenyan
ISP with a Nairobi IP). The `location.lat/lon` from the response is the
**GPS-corrected** location — that's the one to use for the display name.

### Fix in: `WeatherRepository.kt` / `HomeViewModel.kt`

**Option A (preferred): reverse-geocode the GPS coordinates**

Use `Geocoder` to get the city name from `location.lat` / `location.lon` in
the API response (these are accurate), not from `ip_geo.city`.

```kotlin
// In HomeViewModel or Repository:
private fun cityFromCoordinates(lat: Double, lon: Double, context: Context): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        addresses?.firstOrNull()?.locality
            ?: addresses?.firstOrNull()?.subAdminArea
            ?: "Unknown location"
    } catch (e: Exception) {
        "Unknown location"
    }
}
```

**Option B (simpler): use `ip_geo` only as a fallback; prefer device GPS**

```kotlin
// In HomeViewModel:
// 1. Get device GPS location via FusedLocationProviderClient
// 2. Pass those coords to the weather endpoints
// 3. Use Geocoder on THOSE coords for the display city name
// 4. ip_geo.city is never shown directly in the UI
```

**Also add a Settings screen override** (see Section 8 below) so the user can
manually enter their city/coordinates when auto-detection is wrong.

---

## 5. Bug: AI Insight never loads

### Root cause

`HomeViewModel.loadAiSummary()` was making a **separate** network call to fetch
the AI summary. But the AI summary is already included in the main weather
response when `ai=true`. It was just never read.

### Fix in: `HomeViewModel.kt`

```kotlin
// REMOVE the separate loadAiSummary() API call.
// INSTEAD, read it from the weather response after the normal load:

private fun mapResponseToState(response: WeatherResponse): HomeUiState {
    return HomeUiState(
        // … existing fields …
        aiSummary = response.aiSummary?.summary,
        isLoadingAiSummary = false
    )
}

// The "Get insight →" button in AiInsightSection should now
// trigger a re-fetch of the main weather endpoint with ai=true,
// not a separate /ai endpoint call.
// If the summary is null after load, it means the server didn't
// return one (perhaps ai=false was sent) — fix param sending first (Section 1).
```

---

## 6. `StatRow` in HomeScreen — use `current` not `hourly[0]`

### Fix in: `HomeScreen.kt`

`StatRow` was reading humidity, windSpeed, feelsLike from `state.hourly.firstOrNull()`.
Now that the model has all three on `current`, use those directly:

```kotlin
// BEFORE
val currentHourly = state.hourly.firstOrNull()
StatRow(
    humidity  = currentHourly?.humidity?.toInt(),
    windSpeed = state.current?.windSpeed,
    feelsLike = currentHourly?.feelsLike
)

// AFTER — all three come from current
StatRow(
    humidity  = state.current?.humidity?.toInt(),
    windSpeed = state.current?.windSpeed,
    feelsLike = state.current?.feelsLike
)
```

---

## 7. New detail cards — wind gust + precipitation sum

### Fix in: `HomeScreen.kt` — `DetailsGrid` composable

Add two more cards to `DetailsGrid` (already visible in the mockup above):

```kotlin
@Composable
private fun DetailsGrid(state: HomeUiState) {
    val daily  = state.daily.firstOrNull()
    val current = state.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailCard(Modifier.weight(1f), "Sunrise",    daily?.sunrise?.takeLast(5) ?: "--", Icons.Outlined.WbTwilight)
            DetailCard(Modifier.weight(1f), "Sunset",     daily?.sunset?.takeLast(5)  ?: "--", Icons.Outlined.WbTwilight)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailCard(Modifier.weight(1f), "UV Index",   "${current?.uvIndex?.toInt() ?: "--"}", Icons.Outlined.WbSunny)
            DetailCard(Modifier.weight(1f), "Rain chance","${daily?.precipitationProbability?.toInt() ?: "--"}%", Icons.Outlined.Umbrella)
        }
        // NEW ROW
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DetailCard(Modifier.weight(1f), "Wind gust",  "${current?.windGust?.toInt() ?: "--"} km/h", Icons.Outlined.Air)
            DetailCard(Modifier.weight(1f), "Precip. sum","${daily?.precipitationSum?.let { "%.1f mm".format(it) } ?: "--"}", Icons.Outlined.WaterDrop)
        }
    }
}
```

---

## 8. New screen: Settings

Add a new `SettingsScreen.kt` (see companion file `SettingsScreen.kt`).

### Nav change in: `MainScreen.kt`

Replace the `History` tab icon with `Settings`, or add it as a 4th tab.
Recommended: replace `History` with `Settings` in the bottom nav and move
History access to within the Scanner tab.

```kotlin
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Screen("home",     "Home",     Icons.Outlined.Home)
    object Forecast : Screen("forecast", "Forecast", Icons.Outlined.CalendarMonth)
    object Scanner  : Screen("scanner",  "Scanner",  Icons.Outlined.PhotoCamera)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)  // ← REPLACE History
}
```

The Settings screen must expose:

1. **Language selector** — English / Kiswahili (`en` / `sw`). Stored in
   `DataStore`. Passed as `lang` param to all API calls.
2. **Units selector** — Metric / Imperial. Stored in `DataStore`. Passed as
   `units` param.
3. **AI summaries toggle** — on/off. When off, `ai=false` is sent.
4. **Location override** — text field for city name + manual lat/lon entry.
   When set, this overrides the GPS/IP coords. Fixes the Nairobi vs Mombasa bug.
5. **Usage display** — reads from `/v1/usage` endpoint, shows requests used,
   AI requests used, plan name, reset date.

---

## 9. Usage API

### New in: `WeatherApiService.kt`

```kotlin
@GET("v1/usage")
suspend fun getUsage(): UsageResponse
```

### New DTO: `UsageResponse.kt`

```kotlin
data class UsageResponse(
    val plan:    String,
    val period:  UsagePeriod,
    val limits:  UsageLimits,
    val remaining: UsageRemaining
)

data class UsagePeriod(
    val start:          String,
    val end:            String,
    val requestCount:   Int,
    val aiRequestCount: Int
)

data class UsageLimits(
    val requests:   Int,
    val aiRequests: Int,
    val maxDays:    Int,
    val webhooks:   Boolean,
    val teamSeats:  Int,
    val sms:        Boolean
)

data class UsageRemaining(
    val requests:   Int,
    val aiRequests: Int
)
```

Use `maxDays` from the response to clamp the `days` parameter sent in all
weather requests, so free-plan users never accidentally request 14-day data.

---

## 10. `ForecastViewModel` — also request AI summary

```kotlin
// In ForecastViewModel, after fetching daily forecast:
val response = repository.getForecast(lat, lon, days = planMaxDays, ai = true, lang = userLang)
_uiState.update { it.copy(
    daily     = response.daily,
    aiSummary = response.aiSummary?.summary  // ← store for ForecastScreen
) }
```

### Fix in: `ForecastScreen.kt` — add AI summary card

```kotlin
// After ForecastHero(...):
state.aiSummary?.let { summary ->
    AiForecastSummaryCard(summary = summary)  // same pattern as HomeScreen
}
```

---

## 11. `HourlyViewModel` (if separate) — same filter fix as Section 3

Apply the same `filter { !t.isBefore(nowHour) }` logic wherever hourly data
is prepared for display.

---

## Summary checklist

- [ ] `WeatherApiService.kt` — add `ai`, `lang`, `units`, `days` to all endpoints
- [ ] `WeatherResponse.kt` — add `AiSummaryDto`, `IpGeoDto`, missing DTO fields
- [ ] `HomeViewModel.kt` — fix hourly filter; read `aiSummary` from response; use `current` for stats
- [ ] `HomeScreen.kt` — use `current` for StatRow; add Wind Gust + Precip Sum detail cards
- [ ] `ForecastViewModel.kt` — pass `ai=true`; store `aiSummary`
- [ ] `ForecastScreen.kt` — render AI summary card
- [ ] `MainScreen.kt` — add Settings tab
- [ ] NEW `SettingsScreen.kt` — see companion file
- [ ] NEW `SettingsViewModel.kt` — language pref, units pref, location override, usage fetch
- [ ] NEW `UsageResponse.kt` DTO
- [ ] City display — use Geocoder on response `location.lat/lon`, not `ip_geo.city`