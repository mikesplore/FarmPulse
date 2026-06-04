# FarmPulse — UI Structure & Design Reference
**Kotlin + Jetpack Compose | Screen-by-screen breakdown**

---

## Design Language

### Core principle
Every screen should answer one question for the farmer in under 3 seconds. No decoration that doesn't carry meaning. Whitespace is structure.

### Color roles in context

| Color | Hex | When to use |
|-------|-----|-------------|
| Forest Green `#2D6A4F` | Primary | Active nav item, primary button background, AI insight border |
| Light Green `#52B788` | Secondary | Progress bars, healthy tree indicator, quota fill |
| Off-white `#F8F7F2` | App background | Screen background — never pure white |
| White `#FFFFFF` | Card surface | Raised cards only |
| Warm grey `#F0EFEA` | Chip/input bg | Stat chips, input fields, secondary buttons |
| Charcoal `#1C1C1A` | Primary text | Temperatures, headings, key values |
| Muted brown-grey `#6B6B60` | Secondary text | Labels, metadata, condition text |
| Amber `#D4850A` | Warning | Rain probability bars, warning recommendations, offline banner |
| Terracotta `#C45C26` | Danger | "Needs replacement" tree count, critical alert |
| Light border `#E0DFD8` | Dividers | Card borders, row separators |

### Typography scale

| Use | Size | Weight | Color |
|-----|------|--------|-------|
| Screen title | 17sp | 500 | Charcoal |
| Temperature hero | 72sp | 500 | Charcoal |
| Section heading | 11sp | 500 | Muted (uppercase, 0.07em tracking) |
| Card primary value | 15–18sp | 500 | Charcoal or semantic color |
| Body / recommendation text | 13sp | 400 | `#3A3A35` |
| Label / caption | 11–12sp | 400 | Muted brown-grey |
| Bottom nav label | 10sp | 400 active:500 | Muted / Forest Green |

### Spacing system
- Screen horizontal padding: `16dp`
- Card internal padding: `14dp`
- Between cards: `10dp`
- Section heading top margin: `20dp`, bottom: `10dp`
- Bottom nav height: `56dp` + safe area inset

### Component patterns
- **Cards**: white background, `border-radius: 14dp`, `border: 0.5dp #E0DFD8`, no elevation
- **Chips**: warm grey background `#F0EFEA`, `border-radius: 10dp`, no border
- **Stat row**: 3 chips in a `Row` with equal weight, `weight(1f)` each
- **Buttons — primary**: Forest Green fill, white text, `border-radius: 12dp`, `height: 48dp`
- **Buttons — secondary**: transparent, Forest Green border `0.5dp`, Forest Green text
- **Section headings**: all-caps, 11sp, muted, `letterSpacing = 0.07em`
- **Dividers**: `0.5dp` horizontal, `#E0DFD8`

---

## Screen 1 — Home

### Purpose
Show the farmer their current weather at a glance, plus an optional AI insight.

### Layout (top to bottom)

```
StatusBar
OfflineBanner          ← only shown when data is from cache
│  WifiOff icon + "Offline — last updated X hours ago"
│  Background: #FEF3E0 | Text: #B87A12

TempHero (center-aligned)
│  Location row: MapPin icon + "City, Country" (13sp, muted)
│  Temperature: 72sp, Forest Green font color would feel wrong
│    → use Charcoal #1C1C1A, weight 500
│  Condition text: 15sp, muted ("Partly cloudy")

StatRow  ← 3 equal chips
│  Chip 1: Humidity %
│  Chip 2: Wind km/h
│  Chip 3: Feels like °

AiInsightCard          ← visible after user taps "Get insight" OR if ai=true data already loaded
│  Badge: Sparkles icon + "AI insight" (10sp, Forest Green, uppercase)
│  Text: 13sp, Forest Green #2D6A4F, line-height 1.55
│  Background: #EAF4EE | Border: #B8DAC5
│  If not yet loaded: show a ghost/shimmer card with a "Get AI insight →" button

SectionHeading: "Next 6 hours"

HourlyStrip  ← horizontal LazyRow, no scroll indicator
│  Each HourCell:
│    time label (10sp)
│    weather icon (16sp Material Outlined)
│    temperature (13sp, weight 500)
│  Active cell (current hour): Forest Green background, white text/icon
│  Rain hour: amber rain icon (ti-cloud-rain), amber temperature

BottomNavigation
```

### State management
```
HomeUiState(
  isLoading: Boolean,
  weather: WeatherData?,          // from /v1/weather-geo
  hourly: List<HourlyForecast>,   // first 6 items from hourly array
  error: String?,
  isFromCache: Boolean,
  cachedAt: Long?,
  aiSummary: String?,             // only populated after user taps
  isLoadingAiSummary: Boolean
)
```

### Behaviour notes
- On first launch: show skeleton shimmer over TempHero and StatRow while loading
- `isFromCache = true` → show OfflineBanner. When back online, auto-refresh silently
- "Get AI insight" taps → `isLoadingAiSummary = true` → shows loading dots in card → replaces with text
- Pull-to-refresh: only triggers new API call if online AND last fetch was > 30 minutes ago (check `cachedAt`)

---

## Screen 2 — Forecast

### Purpose
Give a full 7-day picture and help the farmer plan around rain days.

### Layout (top to bottom)

```
StatusBar

ScreenTitle: "7-day forecast"
Subtitle: "City, Country" (12sp, muted)

ForecastCard  ← single card containing all 7 rows
│  Each DayRow:
│    dayName (42dp width, 13sp)  ← today shown in Forest Green
│    weatherIcon (16sp, centered)  ← rain icon in Amber
│    PrecipBar (thin 4dp bar, green normal / amber for rain days)
│    hi°/lo° temps (50dp width, right-aligned)
│  DayRow separator: 0.5dp #E0DFD8

RainProbabilitySection
│  SectionHeading: "Rain probability"
│  BarChart card: 7 vertical bars
│    bar color: #52B788 for < 40%, #D4850A for ≥ 60%
│    bars sized relative to each other (max height = 46dp)
│    day label below each bar (9sp)

BottomNavigation
```

### State management
```
ForecastUiState(
  isLoading: Boolean,
  daily: List<DailyForecast>,    // 7 items from /v1/daily
  error: String?
)
```

### Behaviour notes
- Rain day rows: amber condition icon + amber precipitation bar to visually warn
- Today row: day name in Forest Green, slightly bolder
- No hourly data on this screen — keeps it scannable. Hourly lives on Home.
- Empty state: skeleton shimmer on all 7 rows

---

## Screen 3a — Farm Scanner (Empty / Upload state)

### Purpose
Let the farmer upload a farm image and trigger a tree analysis.

### Layout (top to bottom)

```
StatusBar

ScreenTitle: "Farm scanner"

QuotaCard  ← always visible at top
│  Row: "Monthly analyses" label | "2 of 5 used" (Forest Green)
│  QuotaBar: thin 6dp bar, Forest Green fill (width = used/limit %)
│  Sub-label: "3 remaining · resets Jul 1" (11sp, muted)

UploadZone  ← dashed border zone
│  Border: 1.5dp dashed #B8DAC5
│  Background: #F4FAF6
│  Icon: ti-photo-up (28sp, Light Green)
│  Title: "Upload farm image" (14sp, weight 500)
│  Sub: "Drone or satellite photo works best" (12sp, muted)
│  → After image selected: replace with thumbnail preview of selected image

ImageSourceRow  ← two equal chips side by side
│  Chip 1: ti-camera icon + "Camera"
│  Chip 2: ti-photo icon + "Gallery"

OptionalFields
│  Label: "County (optional)"
│  TextField (warm grey background, 9dp vertical padding)
│  Label: "Farm size (acres)"
│  TextField

AnalyzeButton  ← primary
│  Text: "Analyze farm"
│  Disabled state: opacity 0.4, not clickable if no image selected
│  Loading state: CircularProgressIndicator (white, 18dp) inside button

BottomNavigation
```

### State management
```
ScannerUiState(
  selectedImageUri: Uri?,
  farmName: String,
  county: String,
  acres: String,
  isAnalyzing: Boolean,
  result: TreeAnalysisResult?,    // null until analysis completes
  error: String?,
  quotaUsed: Int,
  quotaLimit: Int,
  quotaResetsAt: String
)
```

### Behaviour notes
- If `quotaUsed >= quotaLimit`: disable AnalyzeButton, show message "You've used all analyses this month. Resets Jul 1."
- `isAnalyzing = true` → button shows spinner, upload zone disabled, fields disabled
- `result != null` → navigate to Screen 3b (push, not replace, so back arrow works)

---

## Screen 3b — Analysis Result

### Purpose
Show the complete AI-powered tree health analysis with actionable recommendations.

### Layout (top to bottom)

```
StatusBar

Header row
│  BackArrow (ti-arrow-left, 18sp, Forest Green)
│  Title: "Analysis result" (17sp, weight 500)

OverlayImageCard  ← full-width, 120dp height
│  Background: #DFF0E8 (placeholder until real URL loads)
│  Coil AsyncImage of overlayImageUrl
│  Bottom-right badge: "AI overlay · 47 trees" (white pill, 11sp, Forest Green text)

HealthStatsRow  ← 3 equal cards
│  Green card: healthy count (18sp Forest Green)
│             "Healthy" label (11sp #3A8060)
│  Amber card: needsCare count (18sp Amber)
│             "Needs care" label (11sp #C88A18)
│  Red card:   needsReplacement count (18sp Terracotta)
│             "Replace" label (11sp Terracotta)

MetaStatsRow  ← 2 equal chips
│  Chip 1: canopyCoveragePct% + "Canopy cover"
│  Chip 2: confidenceScore + "Confidence"

SectionHeading: "Recommendations"

RecommendationsCard
│  Each RecItem row:
│    colored dot (6dp circle) ← green for general, amber for urgent
│    recommendation text (12sp, #3A3A35, line-height 1.5)
│  Rows separated by 0.5dp divider

BottomNavigation
```

### Behaviour notes
- OverlayImageCard: show a Forest Green tree icon placeholder while Coil loads the URL
- Recommendation dot color: amber if text contains keywords like "prune", "urgent", "before rain"; green otherwise
- Auto-saved to Room on result receipt — no explicit save button needed (confirm with a subtle Toast: "Saved to history")
- Confidence score: display as-is if 0.0–1.0; multiply by 100 if API returns it as a decimal

---

## Screen 4 — History

### Purpose
Let the farmer review past scans and track farm health over time.

### Layout (top to bottom)

```
StatusBar

ScreenTitle: "Scan history"
Subtitle: "X analyses this month" (12sp, muted)

HistoryCard  ← single card containing all rows
│  Each HistItem row:
│    Thumbnail (40×40dp, rounded 8dp, green tinted background)
│      ti-trees icon (Forest Green)
│    Info column:
│      farm name (13sp, weight 500)
│      treeCount + county + date (11sp, muted)
│    HealthBadge (right-aligned)
│      "Healthy"    → bg #EAF4EE, text #2D6A4F
│      "Needs care" → bg #FEF3E0, text #B87A12
│      "Critical"   → bg #FDE8E3, text #C45C26
│  Rows separated by 0.5dp divider

QuotaFooter
│  "X analyses remaining this month" (11sp, muted, centered)
│  NewScanButton (secondary style) → navigates to Screen 3a

BottomNavigation
```

### Badge health logic
```kotlin
fun determineHealthBadge(result: TreeAnalysisResult): HealthStatus {
    val total = result.totalTreeCount
    if (total == 0) return HealthStatus.UNKNOWN
    val healthyRatio = result.treeHealth.healthy.toFloat() / total
    return when {
        healthyRatio >= 0.70f -> HealthStatus.HEALTHY
        healthyRatio >= 0.45f -> HealthStatus.NEEDS_CARE
        else                  -> HealthStatus.CRITICAL
    }
}
```

### Behaviour notes
- Data source: `TreeAnalysisDao.getAllAnalyses()` as `Flow<List<>>` — auto-updates when new scan is saved
- Empty state: centered illustration placeholder + "No scans yet. Upload your first farm photo." + NewScanButton
- Tap any row → navigate to Screen 3b with the cached result (no new API call)

---

## Bottom Navigation

```
BottomNavigation
├── Home        icon: ti-home       route: "home"
├── Forecast    icon: ti-calendar   route: "forecast"
├── Scanner     icon: ti-camera     route: "scanner"
└── History     icon: ti-history    route: "history"
```

### Active / inactive states
- Active: Forest Green `#2D6A4F` icon + label, label weight 500
- Inactive: Muted `#B0AFA8` icon + label, label weight 400
- No background pill, no badge — keep it flat
- Background: `rgba(248,247,242,0.96)` with top border `0.5dp #E0DFD8`

### Compose implementation
```kotlin
NavigationBar(
    containerColor = Color(0xF7F8F7F2),
    tonalElevation = 0.dp
) {
    NavigationBarItem(
        selected = currentRoute == "home",
        onClick = { navController.navigate("home") },
        icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
        label = { Text("Home", fontSize = 10.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ForestGreen,
            selectedTextColor = ForestGreen,
            unselectedIconColor = Color(0xFFB0AFA8),
            unselectedTextColor = Color(0xFFB0AFA8),
            indicatorColor = Color.Transparent
        )
    )
    // ...repeat for Forecast, Scanner, History
}
```

---

## Offline Banner Component

Shown at the very top of the Home screen (below StatusBar, above content) whenever `isFromCache = true`.

```kotlin
@Composable
fun OfflineBanner(cachedAt: Long) {
    val minutesAgo = ((System.currentTimeMillis() - cachedAt) / 60_000).toInt()
    val label = when {
        minutesAgo < 1   -> "just now"
        minutesAgo < 60  -> "$minutesAgo minutes ago"
        minutesAgo < 120 -> "1 hour ago"
        else             -> "${minutesAgo / 60} hours ago"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF3E0))
            .border(BorderStroke(0.5.dp, Color(0xFFF5D89A)), shape = RectangleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = Color(0xFFD4850A),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Offline · last updated $label",
            fontSize = 12.sp,
            color = Color(0xFFB87A12)
        )
    }
}
```

---

## Loading States

| Screen | Skeleton target | Implementation |
|--------|----------------|----------------|
| Home | TempHero + StatRow + AiCard | 3 shimmer blocks, rounded corners match real layout |
| Forecast | 7 DayRows | 7 shimmer rows in ForecastCard |
| Scanner | Result OverlayImage | Full-width shimmer block + 3 stat shimmer chips |
| History | HistItems | 3 shimmer rows in HistoryCard |

Use `Modifier.shimmer()` from `com.valentinilk.shimmer:shimmer:1.3.0` or implement manually:

```kotlin
@Composable
fun ShimmerBox(modifier: Modifier) {
    val shimmerColors = listOf(
        Color(0xFFEFEFEA),
        Color(0xFFE0DFD8),
        Color(0xFFEFEFEA)
    )
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1000))
    )
    Box(modifier = modifier.background(
        brush = Brush.horizontalGradient(shimmerColors, startX = translateAnim - 300, endX = translateAnim),
        shape = RoundedCornerShape(10.dp)
    ))
}
```

---

## Screen Transition Animations

- Nav tab switch: `fadeIn(tween(150))` + `fadeOut(tween(100))` — no slide, keeps it calm
- Scanner → Result: `slideInHorizontally { it }` + `slideOutHorizontally { -it }` — standard push
- Back from Result: `slideInHorizontally { -it }` + `slideOutHorizontally { it }`

```kotlin
NavHost(
    navController = navController,
    startDestination = "home",
    enterTransition = { fadeIn(tween(150)) },
    exitTransition = { fadeOut(tween(100)) }
) {
    composable("home") { HomeScreen() }
    composable("forecast") { ForecastScreen() }
    composable("scanner") { ScannerScreen() }
    composable("scanner/result") { ResultScreen() }
    composable("history") { HistoryScreen() }
}
```

---

## Summary: What each screen is responsible for

| Screen | API calls | Local reads | Local writes |
|--------|-----------|-------------|--------------|
| Home | `/weather-geo` (launch), `/weather` (AI tap) | `WeatherDao.getCachedWeather()` | `WeatherDao.saveWeather()` |
| Forecast | `/daily` | None | None |
| Scanner | `/trees/analyze`, `/trees/quota` | None | `TreeAnalysisDao.saveAnalysis()` |
| Result | None (result passed via nav) | `TreeAnalysisDao` (on tap from History) | None |
| History | None | `TreeAnalysisDao.getAllAnalyses()` | None |
