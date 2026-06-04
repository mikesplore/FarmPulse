# FarmPulse — Agent Build Plan
**Android (Kotlin + Jetpack Compose) | WeatherAI Free Plan**

---

## Important: Read Before You Start

This document is your complete build guide. Follow the phases in order. Do not skip ahead. Before writing a single line of app code, you must complete Phase 0 — endpoint testing. The response shapes from the API will determine how you model your data classes.

You are building on the **Free plan** of the WeatherAI API. This means:
- Max 1,000 requests/month (use `?ai=false` during development to preserve quota)
- AI summaries: 200/month (only enable `ai=true` in production flows)
- Forecast: up to 7 days
- Tree analyses: 5/month
- No webhooks, no SMS, no `/insights`, no `/forecast14`

Base URL: `https://api.weather-ai.co`
Auth header on every request: `Authorization: Bearer wai_YOUR_KEY_HERE`

---

## Design System

### Philosophy
Minimalistic, agriculture-inspired. The UI should feel calm and trustworthy — a farmer checking this at 6 AM should understand it in 3 seconds. No decorative noise.

### Color Palette

| Role | Color | Hex |
|------|-------|-----|
| Primary (CTAs, active states) | Forest Green | `#2D6A4F` |
| Primary variant | Light Green | `#52B788` |
| Background | Off-white | `#F8F7F2` |
| Surface (cards) | White | `#FFFFFF` |
| Surface variant | Warm grey | `#EFEFEA` |
| On-surface text | Charcoal | `#1C1C1A` |
| Secondary text | Muted brown-grey | `#6B6B60` |
| Accent (warnings, rain) | Amber | `#D4850A` |
| Danger / critical alerts | Terracotta | `#C45C26` |
| Border / divider | Light grey | `#E0DFD8` |

### Typography
- Font: System default (`-apple-system` equivalent → use `FontFamily.Default` in Compose)
- Headings: `fontWeight = FontWeight.SemiBold`
- Body: `fontWeight = FontWeight.Normal`
- Captions / labels: `fontSize = 12.sp`, `color = MutedBrownGrey`

### Component Rules
- Card elevation: `0.dp` — use border (`1.dp`, `#E0DFD8`) instead of shadow
- Corner radius: `12.dp` for cards, `8.dp` for chips/badges, `24.dp` for FABs
- Padding: `16.dp` horizontal screen margin, `12.dp` card internal padding
- No gradients, no blur, no animation beyond simple fade/slide transitions
- Icons: use Material Icons Outlined only

---

## Phase 0 — Endpoint Testing (Do This First)

> **Agent instruction:** Before writing any Kotlin code, test each endpoint below using cURL or a REST client (Postman / HTTP file). Log the full response JSON. You will use the actual response shapes to write your data classes accurately. Do not guess field names.

### 0.1 — Auto-detect weather by IP (primary launch endpoint)

```
GET https://api.weather-ai.co/v1/weather-geo?ip=auto&days=7&ai=false&units=metric
Authorization: Bearer wai_YOUR_KEY
```

**What to record:**
- Top-level keys in the response object
- Structure of the `current` block — field names for temperature, humidity, wind, condition, weather code
- Structure of each item in the `forecast` or `daily` array — date format, high/low temp fields, condition fields
- Structure of each item in the `hourly` array (if present) — time format, temp, precipitation fields
- The `geo` or location block — city, region, country field names
- Whether the AI summary is a top-level field or nested, and its exact key name
- Any response headers: `X-City`, `X-Region`, `X-Country`

**Why this matters:** This endpoint drives the entire home screen. Every data class you write for weather depends on what you find here.

---

### 0.2 — Current conditions only

```
GET https://api.weather-ai.co/v1/current?lat=-1.2921&lon=36.8219&ai=false&units=metric
Authorization: Bearer wai_YOUR_KEY
```

**What to record:**
- Does this return the same shape as `weather-geo` minus the geo block?
- Any fields present here that were absent from `weather-geo`?

---

### 0.3 — Daily forecast

```
GET https://api.weather-ai.co/v1/daily?lat=-1.2921&lon=36.8219&days=7&ai=false&units=metric
Authorization: Bearer wai_YOUR_KEY
```

**What to record:**
- Exact key for the forecast array
- Fields per day: date, tempMax, tempMin, condition, precipitation, humidity, wind
- Date format (ISO string? epoch? `yyyy-MM-dd`?)

---

### 0.4 — Hourly forecast

```
GET https://api.weather-ai.co/v1/hourly?lat=-1.2921&lon=36.8219&days=1&ai=false&units=metric
Authorization: Bearer wai_YOUR_KEY
```

**What to record:**
- Exact key for the hourly array
- Fields per hour: time, temp, precipitation probability, condition
- Time format

---

### 0.5 — AI summary (use sparingly — only 200/month on free plan)

```
GET https://api.weather-ai.co/v1/weather?lat=-1.2921&lon=36.8219&days=1&ai=true&units=metric
Authorization: Bearer wai_YOUR_KEY
```

**What to record:**
- Exact field name and location of the Gemini AI summary string in the response
- Is it a single string or an object?

---

### 0.6 — Tree analysis

```
POST https://api.weather-ai.co/v1/trees/analyze
Authorization: Bearer wai_YOUR_KEY
Content-Type: multipart/form-data

Fields:
  image: [attach any JPEG photo of trees/plants — even a garden photo is fine for testing]
  farmerId: "test-001"
  county: "Nairobi"
  landAcres: "1.0"
  notes: "Test analysis"
```

**What to record:**
- `analysis_id` field name
- `total_tree_count` field name
- `tree_health` block — exact keys for healthy, needs_care, needs_replacement counts
- `canopy_coverage_pct` field name
- `observations` — is it a `List<String>`?
- `recommendations` — is it a `List<String>`?
- `overlay_image_url` — is it a direct public URL? Does it require auth to load?
- `original_image_url` field name
- `confidence_score` — range (0.0–1.0?)

---

### 0.7 — Tree analysis history

```
GET https://api.weather-ai.co/v1/trees/history?limit=5
Authorization: Bearer wai_YOUR_KEY
```

**What to record:**
- Top-level wrapper key for the list
- Shape of each history item — does it include full analysis data or just a summary?
- Pagination: what is the `next_cursor` field called?

---

### 0.8 — Tree quota

```
GET https://api.weather-ai.co/v1/trees/quota
Authorization: Bearer wai_YOUR_KEY
```

**What to record:**
- Fields: `used`, `remaining`, `limit`, `plan`, `resets_at`

---

### 0.9 — Usage stats

```
GET https://api.weather-ai.co/v1/usage
Authorization: Bearer wai_YOUR_KEY
```

**What to record:**
- Full response shape — `requests_used`, `requests_limit`, `ai_used`, `ai_limit`, period fields

---

### ✅ Phase 0 Complete When:
You have a local JSON file or notes doc with a sample response from every endpoint above. Only then proceed to Phase 1.

---

## Phase 1 — Project Setup & Architecture

### 1.1 — Create the project

- New Android project, minimum SDK 26 (Android 8.0)
- Language: Kotlin
- UI toolkit: Jetpack Compose
- Build system: Gradle (Kotlin DSL preferred)
- Package name: `co.farmpulse.app`

### 1.2 — Add dependencies to `build.gradle.kts`

```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Lifecycle + ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// Local database (offline cache)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// DataStore (for API key + preferences)
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Image loading (for overlay images from tree analysis)
implementation("io.coil-kt:coil-compose:2.5.0")

// Location
implementation("com.google.android.gms:play-services-location:21.1.0")

// Permissions
implementation("com.google.accompanist:accompanist-permissions:0.34.0")
```

### 1.3 — Folder structure

Create this exact structure. Do not deviate:

```
app/src/main/java/co/farmpulse/app/
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── FarmPulseDatabase.kt
│   │   │   ├── WeatherDao.kt
│   │   │   └── TreeAnalysisDao.kt
│   │   └── entities/
│   │       ├── CachedWeatherEntity.kt
│   │       └── CachedTreeAnalysisEntity.kt
│   ├── remote/
│   │   ├── api/
│   │   │   ├── WeatherApiService.kt
│   │   │   └── TreeApiService.kt
│   │   └── dto/
│   │       ├── WeatherGeoResponse.kt
│   │       ├── CurrentWeatherDto.kt
│   │       ├── DailyForecastDto.kt
│   │       ├── HourlyForecastDto.kt
│   │       └── TreeAnalysisResponse.kt
│   └── repository/
│       ├── WeatherRepository.kt
│       └── TreeRepository.kt
├── domain/
│   ├── model/
│   │   ├── WeatherData.kt
│   │   ├── DailyForecast.kt
│   │   ├── HourlyForecast.kt
│   │   └── TreeAnalysisResult.kt
│   └── usecase/
│       ├── GetWeatherByLocationUseCase.kt
│       ├── GetDailyForecastUseCase.kt
│       ├── GetHourlyForecastUseCase.kt
│       └── AnalyzeTreesUseCase.kt
├── presentation/
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── forecast/
│   │   ├── ForecastScreen.kt
│   │   └── ForecastViewModel.kt
│   ├── scanner/
│   │   ├── ScannerScreen.kt
│   │   └── ScannerViewModel.kt
│   ├── history/
│   │   ├── HistoryScreen.kt
│   │   └── HistoryViewModel.kt
│   └── components/
│       ├── WeatherCard.kt
│       ├── HourlyChart.kt
│       ├── DailyForecastRow.kt
│       ├── TreeHealthCard.kt
│       └── OfflineBanner.kt
├── ui/
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── util/
│   ├── NetworkMonitor.kt
│   ├── LocationHelper.kt
│   └── Extensions.kt
├── di/
│   └── AppModule.kt          ← Hilt or manual DI, your choice
└── MainActivity.kt
```

### 1.4 — Define the color palette in `Color.kt`

```kotlin
val ForestGreen = Color(0xFF2D6A4F)
val LightGreen = Color(0xFF52B788)
val BackgroundOffWhite = Color(0xFFF8F7F2)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFEFEFEA)
val OnSurfaceCharcoal = Color(0xFF1C1C1A)
val SecondaryText = Color(0xFF6B6B60)
val AccentAmber = Color(0xFFD4850A)
val DangerTerracotta = Color(0xFFC45C26)
val BorderGrey = Color(0xFFE0DFD8)
```

### 1.5 — `AndroidManifest.xml` permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<!-- For Android 12 and below: -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
```

---

## Phase 2 — Network Layer

> Agent instruction: Use the actual response JSON from Phase 0 to write these DTOs. Field names must match the API exactly. Use `@SerializedName` for any field that doesn't match Kotlin naming conventions.

### 2.1 — Retrofit client

Create `RetrofitClient.kt`:

```kotlin
object RetrofitClient {
    private const val BASE_URL = "https://api.weather-ai.co/"

    fun create(apiKey: String): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // tree analysis can be slow
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
```

### 2.2 — API service interfaces

`WeatherApiService.kt`:
```kotlin
interface WeatherApiService {

    @GET("v1/weather-geo")
    suspend fun getWeatherByIp(
        @Query("ip") ip: String = "auto",
        @Query("days") days: Int = 7,
        @Query("ai") ai: Boolean = false,
        @Query("units") units: String = "metric"
    ): WeatherGeoResponse

    @GET("v1/daily")
    suspend fun getDailyForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("days") days: Int = 7,
        @Query("ai") ai: Boolean = false,
        @Query("units") units: String = "metric"
    ): DailyForecastResponse

    @GET("v1/hourly")
    suspend fun getHourlyForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("days") days: Int = 1,
        @Query("ai") ai: Boolean = false,
        @Query("units") units: String = "metric"
    ): HourlyForecastResponse

    @GET("v1/current")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("ai") ai: Boolean = false,
        @Query("units") units: String = "metric"
    ): CurrentWeatherResponse

    // Called ONLY for the AI summary — use sparingly
    @GET("v1/weather")
    suspend fun getWeatherWithAiSummary(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("days") days: Int = 1,
        @Query("ai") ai: Boolean = true,
        @Query("units") units: String = "metric"
    ): WeatherGeoResponse

    @GET("v1/usage")
    suspend fun getUsageStats(): UsageResponse
}
```

`TreeApiService.kt`:
```kotlin
interface TreeApiService {

    @Multipart
    @POST("v1/trees/analyze")
    suspend fun analyzeTrees(
        @Part image: MultipartBody.Part,
        @Part("farmerId") farmerId: RequestBody,
        @Part("county") county: RequestBody,
        @Part("landAcres") landAcres: RequestBody,
        @Part("notes") notes: RequestBody
    ): TreeAnalysisResponse

    @GET("v1/trees/history")
    suspend fun getHistory(
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): TreeHistoryResponse

    @GET("v1/trees/quota")
    suspend fun getQuota(): TreeQuotaResponse
}
```

### 2.3 — DTO classes

> Agent instruction: Write these classes AFTER Phase 0. Use the actual JSON field names you recorded. The template below shows structure — replace field names with what you actually found.

```kotlin
// Use the real field names from your Phase 0 test results
data class WeatherGeoResponse(
    @SerializedName("current") val current: CurrentWeatherDto,
    @SerializedName("forecast")  val forecast: List<DailyForecastDto>,  // verify key name
    @SerializedName("hourly")    val hourly: List<HourlyForecastDto>?,   // may be absent
    @SerializedName("ai_summary") val aiSummary: String?,                // verify key name
    @SerializedName("geo")       val geo: GeoDto?                        // verify key name
)

data class GeoDto(
    @SerializedName("city")    val city: String,
    @SerializedName("region")  val region: String,
    @SerializedName("country") val country: String,
    @SerializedName("lat")     val lat: Double,
    @SerializedName("lon")     val lon: Double
)

data class CurrentWeatherDto(
    // Fill these in from Phase 0 results:
    // temperature field name, humidity, wind speed, condition text, weather code/icon
)

data class DailyForecastDto(
    // Fill these in from Phase 0 results:
    // date, temp max, temp min, condition, precipitation, humidity
)

data class HourlyForecastDto(
    // Fill these in from Phase 0 results:
    // time, temperature, precipitation probability, condition
)

data class TreeAnalysisResponse(
    @SerializedName("analysis_id")        val analysisId: String,
    @SerializedName("total_tree_count")   val totalTreeCount: Int,
    @SerializedName("canopy_coverage_pct") val canopyCoveragePct: Double,
    @SerializedName("confidence_score")   val confidenceScore: Double,
    @SerializedName("tree_health")        val treeHealth: TreeHealthDto,
    @SerializedName("observations")       val observations: List<String>,
    @SerializedName("recommendations")    val recommendations: List<String>,
    @SerializedName("overlay_image_url")  val overlayImageUrl: String,
    @SerializedName("original_image_url") val originalImageUrl: String,
    @SerializedName("timestamp")          val timestamp: String,
    @SerializedName("county")             val county: String?,
    @SerializedName("land_acres")         val landAcres: Double?
)

data class TreeHealthDto(
    @SerializedName("healthy")            val healthy: Int,
    @SerializedName("needs_care")         val needsCare: Int,
    @SerializedName("needs_replacement")  val needsReplacement: Int
)
```

---

## Phase 3 — Local Database (Offline Cache)

### 3.1 — Room entities

`CachedWeatherEntity.kt`:
```kotlin
@Entity(tableName = "cached_weather")
data class CachedWeatherEntity(
    @PrimaryKey val id: Int = 1,       // single row — always overwrite
    val city: String,
    val region: String,
    val lat: Double,
    val lon: Double,
    val json: String,                   // store full response as JSON string
    val fetchedAt: Long                 // System.currentTimeMillis()
)
```

`CachedTreeAnalysisEntity.kt`:
```kotlin
@Entity(tableName = "tree_analyses")
data class CachedTreeAnalysisEntity(
    @PrimaryKey val analysisId: String,
    val json: String,
    val createdAt: Long
)
```

### 3.2 — DAOs

```kotlin
@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeather(entity: CachedWeatherEntity)

    @Query("SELECT * FROM cached_weather WHERE id = 1")
    suspend fun getCachedWeather(): CachedWeatherEntity?
}

@Dao
interface TreeAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAnalysis(entity: CachedTreeAnalysisEntity)

    @Query("SELECT * FROM tree_analyses ORDER BY createdAt DESC")
    fun getAllAnalyses(): Flow<List<CachedTreeAnalysisEntity>>
}
```

### 3.3 — Repository pattern (offline-first)

`WeatherRepository.kt`:
```kotlin
class WeatherRepository(
    private val api: WeatherApiService,
    private val dao: WeatherDao,
    private val networkMonitor: NetworkMonitor
) {
    suspend fun getWeather(): Result<WeatherData> {
        return if (networkMonitor.isOnline()) {
            try {
                val response = api.getWeatherByIp()
                val entity = CachedWeatherEntity(
                    city = response.geo?.city ?: "Unknown",
                    region = response.geo?.region ?: "",
                    lat = response.geo?.lat ?: 0.0,
                    lon = response.geo?.lon ?: 0.0,
                    json = Gson().toJson(response),
                    fetchedAt = System.currentTimeMillis()
                )
                dao.saveWeather(entity)
                Result.success(response.toDomain())
            } catch (e: Exception) {
                loadFromCache()
            }
        } else {
            loadFromCache()
        }
    }

    private suspend fun loadFromCache(): Result<WeatherData> {
        val cached = dao.getCachedWeather()
        return if (cached != null) {
            val response = Gson().fromJson(cached.json, WeatherGeoResponse::class.java)
            Result.success(response.toDomain().copy(isFromCache = true, cachedAt = cached.fetchedAt))
        } else {
            Result.failure(Exception("No cached data available"))
        }
    }
}
```

---

## Phase 4 — Screens

### Screen 1: Home Screen

**What it shows:**
- Location name (city, region) from geo response
- Current temperature (large, prominent)
- Weather condition label
- Humidity, wind speed, "feels like" in a row of 3 chips
- Gemini AI summary (shown as a card below conditions) — loaded lazily on a button tap to save quota
- Offline banner at top if data is from cache, showing "Last updated X hours ago"

**ViewModel state:**
```kotlin
data class HomeUiState(
    val isLoading: Boolean = false,
    val weather: WeatherData? = null,
    val error: String? = null,
    val isFromCache: Boolean = false,
    val cachedAt: Long? = null,
    val aiSummary: String? = null,
    val isLoadingAiSummary: Boolean = false
)
```

**Key behaviour:**
- On launch, call `getWeatherByIp()` with `ai=false`
- "Get AI Insight" button triggers a separate call with `ai=true` to the same lat/lon
- Pull-to-refresh refreshes weather only if online

---

### Screen 2: Forecast Screen

**What it shows:**
- Horizontal scrollable hourly strip for next 24 hours (time, temperature, rain icon if probability > 30%)
- 7-day daily forecast list: day name, condition icon, high/low temp, precipitation bar

**ViewModel state:**
```kotlin
data class ForecastUiState(
    val isLoading: Boolean = false,
    val hourly: List<HourlyForecast> = emptyList(),
    val daily: List<DailyForecast> = emptyList(),
    val error: String? = null
)
```

---

### Screen 3: Farm Scanner Screen

**What it shows:**
- Quota banner: "X of 5 analyses used this month"
- Two buttons: "Take Photo" / "Choose from Gallery"
- Optional fields: Farm name, County, Acreage (simple TextFields, not required)
- "Analyze Farm" button (disabled if no image selected)
- On result: annotated overlay image (full width), tree count headline, health breakdown (3 coloured pills: green/amber/terracotta), observations list, recommendations list
- "Save to History" is automatic — result is persisted to Room on receipt

**ViewModel state:**
```kotlin
data class ScannerUiState(
    val selectedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val result: TreeAnalysisResult? = null,
    val error: String? = null,
    val quotaUsed: Int = 0,
    val quotaLimit: Int = 5
)
```

**Image upload flow:**
```kotlin
// In ViewModel
suspend fun analyzeImage(context: Context, uri: Uri, county: String, acres: String) {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bytes = inputStream?.readBytes() ?: return
    val requestBody = bytes.toRequestBody("image/*".toMediaType())
    val imagePart = MultipartBody.Part.createFormData("image", "farm.jpg", requestBody)
    // ... call api.analyzeTrees(imagePart, ...)
}
```

---

### Screen 4: History Screen

**What it shows:**
- List of past tree analyses from Room database
- Each row: date, farm name/county, tree count, health score
- Tap to expand full analysis detail

---

### Navigation structure

```
BottomNavigation
├── Home (icon: WbSunny)
├── Forecast (icon: CalendarMonth)
├── Scanner (icon: CameraAlt)
└── History (icon: History)
```

Use `androidx.navigation:navigation-compose`. Bottom nav with 4 destinations.

---

## Phase 5 — Offline & Connectivity

### `NetworkMonitor.kt`

```kotlin
class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnlineFlow: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapability.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        trySend(connectivityManager.activeNetwork != null)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    fun isOnline(): Boolean =
        connectivityManager.activeNetwork != null
}
```

### Offline banner component

```kotlin
@Composable
fun OfflineBanner(cachedAt: Long) {
    val timeAgo = remember(cachedAt) { getTimeAgo(cachedAt) }
    Surface(color = AccentAmber.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = AccentAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Offline — last updated $timeAgo",
                fontSize = 13.sp,
                color = AccentAmber
            )
        }
    }
}
```

---

## Phase 6 — API Key Management

Do NOT hardcode the API key in source code. Use one of these approaches:

**Option A — `local.properties` (simplest, keeps key out of Git):**

In `local.properties` (gitignored):
```
weatherai.api.key=wai_your_actual_key
```

In `build.gradle.kts`:
```kotlin
val apiKey = properties["weatherai.api.key"] as String? ?: ""
buildConfigField("String", "WEATHER_AI_KEY", "\"$apiKey\"")
```

Access in code: `BuildConfig.WEATHER_AI_KEY`

**Option B — DataStore (user enters key in settings):**
Store via `DataStore<Preferences>` and read before initialising the Retrofit client. This is the better approach if you want to demo without exposing your key in the APK.

---

## Phase 7 — README & Submission

### README.md must include:

1. **App name and tagline** — "FarmPulse — AI-powered weather and farm health for Kenyan farmers"
2. **Architecture diagram** — simple text diagram showing Data → Domain → Presentation layers
3. **Features list** with which API endpoint powers each feature
4. **Screenshots** — at least 4 (Home, Forecast, Scanner with result, History)
5. **Offline behaviour** — explain the caching strategy explicitly
6. **Setup instructions** — how to add API key via local.properties and run
7. **APK download link** — Google Drive or Firebase App Distribution
8. **Known limitations** — free plan constraints (5 tree analyses/month, 7-day forecast, 200 AI summaries)

### APK build:

```bash
./gradlew assembleRelease
```

Sign with a keystore. Upload signed APK to Google Drive, set sharing to "Anyone with the link can view."

---

## Quota Management Reference (Free Plan)

| Endpoint | Monthly limit | Dev tip |
|----------|--------------|---------|
| All weather endpoints | 1,000 requests | Always pass `ai=false` during development |
| AI summaries (`ai=true`) | 200 requests | Only call on explicit user tap |
| Tree analyses | 5 analyses | Test with the same image; save results to Room |
| `/v1/usage` | Counts against quota | Check it once at app start |

**Rule:** Never make automatic background syncs that loop. Refresh weather on app foreground only, max once every 30 minutes (check `fetchedAt` in Room before deciding to call the API).

---

## Phase Order Summary

| Phase | Task | Done when |
|-------|------|-----------|
| 0 | Test all 9 endpoints, record response shapes | JSON samples saved locally |
| 1 | Project setup, dependencies, folder structure, colors | App compiles |
| 2 | Retrofit client, API interfaces, DTOs from real responses | Network calls return data |
| 3 | Room database, DAOs, repositories with offline fallback | Data survives airplane mode |
| 4 | All 4 screens + ViewModels + navigation | Full user flow works |
| 5 | NetworkMonitor + offline banner + quota guard | App works offline |
| 6 | API key management via local.properties | Key not in source |
| 7 | README, screenshots, signed APK, submission | Email sent to Claire |