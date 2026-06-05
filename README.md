# FarmPulse — AI-powered weather and farm health for Kenyan farmers

FarmPulse is a modern Android application designed to empower Kenyan farmers with hyper-local weather insights and AI-driven farm health diagnostics. By leveraging high-resolution weather data and computer vision, FarmPulse helps farmers make informed decisions about planting, irrigation, and crop care.

## 🏗 Architecture
The app follows a clean architecture pattern, ensuring separation of concerns and maintainability across three distinct layers:

**Data** → **Domain** → **Presentation**

- **Data Layer**: Responsible for all data sources. Includes a **Room Database** for local caching, **Retrofit** for network communication with the Weather AI API, and **DataStore** for persisting user preferences like API keys.
- **Domain Layer**: Contains the core business logic and pure Kotlin models. It acts as the bridge between raw data and what the user sees.
- **Presentation Layer**: Built entirely with **Jetpack Compose**. ViewModels manage UI state and observe data flows from the domain layer.

## ✨ Features & API Integration
| Feature | API Endpoint | Description |
|:--- |:--- |:--- |
| **Hyper-local Weather** | `/v1/weather-geo` & `/v1/weather` | Discovers location via IP and fetches detailed current/hourly conditions. |
| **7-Day Forecast** | `/v1/weather` | Provides a comprehensive 7-day outlook including rain probability charts. |
| **Tree Scanner** | `/v1/trees/analyze` | Uses computer vision to count trees and assess health from farm photos. |
| **Scan History** | `/v1/trees/history` | Maintains a local record of all past farm health analyses. |
| **AI Weather Summaries** | `/v1/weather` (ai=true) | Generates natural language weather insights in English or Swahili. |
| **Usage Tracking** | `/v1/usage` | Monitors API quota and plan limits in real-time. |

## 📸 Screenshots
### Home Screen
![Home Screen](app/src/main/res/drawable/homescreen.png)

## 📡 Offline Behaviour
FarmPulse implements a **Single Source of Truth (SSOT)** strategy to ensure reliability in remote farming areas:
- **Caching**: All weather and tree analysis data is automatically cached in a local **Room Database**.
- **Persistence**: When the device is offline, the app seamlessly serves the last-known cached data.
- **Visual Feedback**: An offline banner appears on the Home screen showing the exact time the data was last synchronized.
- **Reliability**: Network requests are performed with a fallback mechanism; if a fetch fails, the app restores the UI state from the local cache.

## 🚀 Setup Instructions
To get started with FarmPulse:

1. **Get an API Key**: Sign up at [weather-ai.co](https://weather-ai.co) to obtain your personal developer key.
2. **Install the App**: Build and run the app on your Android device or emulator.
3. **Configure API Key**: 
   - Open the app and you will see the setup page requesting the api key.
   - Paste your key into the text field and press save and continue button.
   - The app will automatically validate the key and navigate to the home screen.

## 📥 APK Download
[Download FarmPulse APK](https://github.com/mikesplore/FarmPulse/releases/download/V1.0/FarmPulse.apk)

## ⚠️ Known Limitations (Free Plan)
The app operates under the following constraints for the free tier:
- **Tree Analysis**: Limited to 5 analyses per month.
- **Forecast Range**: 7 days of forecast data.
- **AI Summaries**: Limited to 200 summary generations per month.
- **Update Frequency**: Data is refreshed every 15 minutes by default.
