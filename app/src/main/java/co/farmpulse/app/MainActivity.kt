package co.farmpulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import co.farmpulse.app.ui.theme.FarmPulseTheme
import co.farmpulse.app.presentation.home.HomeViewModel
import co.farmpulse.app.presentation.forecast.ForecastViewModel
import co.farmpulse.app.presentation.history.HistoryViewModel
import co.farmpulse.app.presentation.scanner.ScannerViewModel
import co.farmpulse.app.presentation.main.MainScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val forecastViewModel: ForecastViewModel by viewModels()
    private val scannerViewModel: ScannerViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FarmPulseTheme(darkTheme = false) { // Design reference uses off-white background
                MainScreen(
                    homeViewModel = homeViewModel,
                    forecastViewModel = forecastViewModel,
                    scannerViewModel = scannerViewModel,
                    historyViewModel = historyViewModel
                )
            }
        }
    }
}
