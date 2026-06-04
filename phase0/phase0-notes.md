# FarmPulse Phase 0 Endpoint Notes

Base URL tested: `https://api.weather-ai.co`

Auth header used on all requests:

```http
Authorization: Bearer wai_79f9ed.a77d9f8e75a521331037bbb4eb0bceb21ad411e6b41f3e83
```

Raw JSON samples are saved in this folder:
- `phase0/weather_geo.json`
- `phase0/current.json`
- `phase0/daily.json`
- `phase0/hourly.json`
- `phase0/weather_ai.json`
- `phase0/tree_analyze.json`
- `phase0/tree_history.json`
- `phase0/tree_quota.json`
- `phase0/usage.json`

Additional re-captured samples and headers:
- `phase0/weather_geo2.json`
- `phase0/weather_geo.headers.txt`
- `phase0/weather_ai2.json`
- `phase0/weather_ai.headers.txt`

## 0.1 Weather by IP (`/v1/weather-geo`)

Top-level keys:
- `location`
- `current`
- `hourly`
- `daily`
- `client_geo`
- `ip_geo`

Observed shapes:

### `location`
- `lat`: number
- `lon`: number
- `timezone`: string
- `requested_lat`: number
- `requested_lon`: number
- `country`: string (`KE`)

### `current`
- `time`: string (`2026-06-04T13:45`)
- `temperature`: number
- `wind_speed`: number
- `wind_direction`: number
- `condition_code`: string
- `icon`: string URL
- `icon_path`: string

### `hourly` item
- `time`: string (`yyyy-MM-ddTHH:mm`)
- `temperature`: number
- `precipitation_probability`: number
- `wind_speed`: number
- `condition_code`: string
- `icon`: string URL
- `humidity`: number
- `feels_like`: number
- `wind_gust`: number
- `uv_index`: number
- `icon_path`: string

Observed array length: `168` for `days=7`

### `daily` item
- `date`: string (`yyyy-MM-dd`)
- `temp_min`: number
- `temp_max`: number
- `precipitation_sum`: number
- `sunrise`: string (`yyyy-MM-ddTHH:mm`)
- `sunset`: string (`yyyy-MM-ddTHH:mm`)
- `condition_code`: string
- `icon`: string URL
- `precipitation_probability`: number
- `wind_max`: number
- `icon_path`: string

Observed array length: `7` for `days=7`

### `client_geo`
- `country`: string (`ZZ`)
- `ip_hash`: string

### `ip_geo`
- `country`: string (`KE`)
- `region`: string (`Nairobi County`)
- `city`: string (`Nairobi`)
- `lat`: number
- `lon`: number
- `asn`: number
- `org`: string
- `ip_hash`: string
- `source`: string (`maxmind`)

### Headers observed
- `x-country: ZZ`
- `x-country-code: KE`
- `x-plan: free`
- `x-ai-requested: false`
- `x-ai-applied: false`
- No `X-City` or `X-Region` header observed in the captured response.

## 0.2 Current conditions (`/v1/current`)

Response shape matched the `weather-geo` weather payload structure:
- `location`
- `current`
- `hourly`
- `daily`
- `client_geo`

No `ip_geo` block returned.

## 0.3 Daily forecast (`/v1/daily`)

Response shape matched the `weather-geo` payload structure:
- `location`
- `current`
- `hourly`
- `daily`
- `client_geo`

Observed array lengths:
- `daily`: `7`
- `hourly`: `168`

## 0.4 Hourly forecast (`/v1/hourly`)

Response shape matched the `weather-geo` payload structure:
- `location`
- `current`
- `hourly`
- `daily`
- `client_geo`

Observed array lengths:
- `hourly`: `24` for `days=1`
- `daily`: `1`

## 0.5 AI summary weather endpoint (`/v1/weather?ai=true`)

Observed request headers:
- `x-ai-requested: true`
- `x-ai-applied: false`
- `x-ai-allow: false`
- `x-ai-env: false`

Observed body shape was the same as the non-AI weather response:
- `location`
- `current`
- `hourly`
- `daily`
- `client_geo`

No AI summary string field was present in the captured body.

## 0.6 Tree analysis (`/v1/trees/analyze`)

Top-level keys:
- `analysis_id`
- `timestamp`
- `farmer_id`
- `county`
- `location`
- `land_acres`
- `total_tree_count`
- `tree_density_per_acre`
- `confidence_score`
- `canopy_coverage_pct`
- `tree_health`
- `low_confidence`
- `tree_species_guess`
- `image_perspective`
- `coverage_estimate`
- `cv_count_used`
- `observations`
- `recommendations`
- `original_image_url`
- `overlay_image_url`
- `cv_debug`
- `gemini_error`

Observed values in this test:
- `analysis_id`: string
- `total_tree_count`: number
- `confidence_score`: number (`0` in this test)
- `canopy_coverage_pct`: `null` in this test
- `tree_health`: object
- `observations`: array (empty in this test)
- `recommendations`: array (empty in this test)
- `original_image_url`: public Google Cloud Storage URL
- `overlay_image_url`: `null` in this test
- `gemini_error`: present with message `GEMINI_API_KEY not configured in functions environment`

### `tree_health`
- `healthy`: number
- `needs_care`: number
- `needs_replacement`: number

### Notes
- The returned object includes more fields than the template in the build plan.
- `farmer_id` uses snake case.
- `land_acres` is numeric in the response.
- `overlay_image_url` can be `null`.

## 0.7 Tree history (`/v1/trees/history?limit=5`)

Top-level keys:
- `analyses`
- `next_cursor`

Each `analyses[]` item observed in the response contained a summary subset of the analysis data, including:
- `analysis_id`
- `timestamp`
- `farmer_id`
- `county`
- `location`
- `land_acres`
- `total_tree_count`
- `tree_density_per_acre`
- `confidence_score`
- `canopy_coverage_pct`
- `overlay_image_url`
- `original_image_url`

`next_cursor` was `null` in this test.

## 0.8 Tree quota (`/v1/trees/quota`)

Top-level keys:
- `plan`
- `used`
- `limit`
- `remaining`
- `unlimited`
- `resets_at`

Observed values:
- `plan`: `free`
- `used`: `1`
- `limit`: `5`
- `remaining`: `4`
- `unlimited`: `false`
- `resets_at`: ISO timestamp string

## 0.9 Usage stats (`/v1/usage`)

Top-level keys:
- `plan`
- `period`
- `limits`
- `remaining`

### `period`
- `start`: ISO timestamp string
- `end`: ISO timestamp string
- `requestCount`: number
- `aiRequestCount`: number

### `limits`
- `requests`: number (`1000`)
- `aiRequests`: number (`200`)
- `maxDays`: number (`7`)
- `webhooks`: boolean (`false`)
- `teamSeats`: number (`1`)
- `sms`: boolean (`false`)

### `remaining`
- `requests`: number
- `aiRequests`: number

## Phase 0 status
Phase 0 is now kicked off and the required endpoint samples are saved locally.
Next step: use these shapes to write the Kotlin DTOs exactly, without guessing field names.

