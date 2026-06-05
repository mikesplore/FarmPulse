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
import co.farmpulse.app.presentation.settings.SettingsViewModel
import co.farmpulse.app.presentation.main.MainScreen
import co.farmpulse.app.util.SnackbarManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val forecastViewModel: ForecastViewModel by viewModels()
    private val scannerViewModel: ScannerViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var snackbarManager: SnackbarManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FarmPulseTheme(darkTheme = false) {
                MainScreen(
                    homeViewModel = homeViewModel,
                    forecastViewModel = forecastViewModel,
                    scannerViewModel = scannerViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel,
                    snackbarManager = snackbarManager
                )
            }
        }
    }
}