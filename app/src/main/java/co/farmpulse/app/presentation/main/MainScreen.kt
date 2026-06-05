package co.farmpulse.app.presentation.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.farmpulse.app.presentation.forecast.ForecastScreen
import co.farmpulse.app.presentation.forecast.ForecastViewModel
import co.farmpulse.app.presentation.history.HistoryScreen
import co.farmpulse.app.presentation.history.HistoryViewModel
import co.farmpulse.app.presentation.home.HomeScreen
import co.farmpulse.app.presentation.home.HomeViewModel
import co.farmpulse.app.presentation.scanner.AnalysisResultScreen
import co.farmpulse.app.presentation.scanner.ScannerScreen
import co.farmpulse.app.presentation.scanner.ScannerViewModel
import co.farmpulse.app.presentation.settings.SettingsScreen
import co.farmpulse.app.presentation.settings.SettingsViewModel
import co.farmpulse.app.ui.theme.*
import co.farmpulse.app.util.SnackbarManager
import kotlinx.coroutines.flow.collectLatest

// ─────────────────────────────────────────────────────────────────────────────
// Nav destinations
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Screen("home",     "Home",     Icons.Outlined.Home)
    object Forecast : Screen("forecast", "Forecast", Icons.Outlined.CalendarMonth)
    object Scanner  : Screen("scanner",  "Scanner",  Icons.Outlined.PhotoCamera)
    object History  : Screen("history",  "History",  Icons.Outlined.History)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
}

private val bottomNavScreens = listOf(
    Screen.Home, Screen.Forecast, Screen.Scanner, Screen.History, Screen.Settings
)

// ─────────────────────────────────────────────────────────────────────────────
// MainScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    homeViewModel:     HomeViewModel,
    forecastViewModel: ForecastViewModel,
    scannerViewModel:  ScannerViewModel,
    historyViewModel:  HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    snackbarManager:   SnackbarManager // Pass the global SnackbarManager
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe global snackbar messages
    LaunchedEffect(snackbarManager.messages) {
        snackbarManager.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        containerColor = BackgroundOffWhite,   // #F8F7F2
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FarmPulseBottomNav(
                navController     = navController,
                screens           = bottomNavScreens
            )
        }
    ) { innerPadding ->
        NavHost(
            navController  = navController,
            startDestination = Screen.Home.route,
            modifier       = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec  = tween(150)) },
            exitTransition  = { fadeOut(animationSpec = tween(100)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(homeViewModel)
            }

            composable(Screen.Forecast.route) {
                ForecastScreen(forecastViewModel)
            }

            composable(Screen.Scanner.route) {
                ScannerScreen(
                    viewModel          = scannerViewModel,
                    onNavigateToResult = { 
                        historyViewModel.selectItem(null)
                        navController.navigate("scanner/result") 
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(settingsViewModel)
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel          = historyViewModel,
                    onNavigateToResult = { result ->
                        scannerViewModel.clearResult()
                        historyViewModel.selectItem(result)
                        navController.navigate("scanner/result")
                    },
                    onNewScan          = { navController.navigate(Screen.Scanner.route) }
                )
            }

            // Scanner result — horizontal slide push (per spec)
            composable(
                route = "scanner/result",
                enterTransition    = { slideInHorizontally(initialOffsetX = { it },  animationSpec = tween(300)) },
                exitTransition     = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
                popExitTransition  = { slideOutHorizontally(targetOffsetX = { it },  animationSpec = tween(300)) }
            ) {
                val scannerState by scannerViewModel.uiState.collectAsState()
                val historyState by historyViewModel.uiState.collectAsState()

                // Prefer the freshly-analysed result; fall back to the explicitly selected history item
                val result = scannerState.result ?: historyState.selectedItem

                if (result != null) {
                    AnalysisResultScreen(
                        result = result,
                        onBack = { 
                            scannerViewModel.clearResult()
                            historyViewModel.selectItem(null)
                            navController.popBackStack() 
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom navigation bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FarmPulseBottomNav(
    navController: androidx.navigation.NavHostController,
    screens: List<Screen>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Column {
        HorizontalDivider(
            thickness = 0.5.dp,
            color     = BorderGrey     // #E0DFD8
        )

        NavigationBar(
            containerColor  = BackgroundOffWhite,
            tonalElevation  = 0.dp
        ) {
            screens.forEach { screen ->
                val isSelected = currentDestination
                    ?.hierarchy
                    ?.any { it.route == screen.route } == true

                NavigationBarItem(
                    selected = isSelected,
                    onClick  = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector        = screen.icon,
                            contentDescription = screen.label,
                            modifier           = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text       = screen.label,
                            fontSize   = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = ForestGreen,
                        selectedTextColor   = ForestGreen,
                        unselectedIconColor = Color(0xFFB0AFA8),
                        unselectedTextColor = Color(0xFFB0AFA8),
                        indicatorColor      = Color.Transparent
                    )
                )
            }
        }
    }
}
