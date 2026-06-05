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
import co.farmpulse.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Nav destinations
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Screen("home",     "Home",     Icons.Outlined.Home)
    object Forecast : Screen("forecast", "Forecast", Icons.Outlined.CalendarMonth)
    object Scanner  : Screen("scanner",  "Scanner",  Icons.Outlined.PhotoCamera)
    object History  : Screen("history",  "History",  Icons.Outlined.History)
}

private val bottomNavScreens = listOf(
    Screen.Home, Screen.Forecast, Screen.Scanner, Screen.History
)

// ─────────────────────────────────────────────────────────────────────────────
// MainScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    homeViewModel:     HomeViewModel,
    forecastViewModel: ForecastViewModel,
    scannerViewModel:  ScannerViewModel,
    historyViewModel:  HistoryViewModel
) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = BackgroundOffWhite,   // #F8F7F2 — Scaffold bg matches screen bg
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
            // Default transitions: fade for tab switches (calm, per spec)
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
                    onNavigateToResult = { navController.navigate("scanner/result") }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel          = historyViewModel,
                    onNavigateToResult = {
                        // ViewModel already holds the selected result; just navigate
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
                val historyList  by historyViewModel.history.collectAsState()

                // Prefer the freshly-analysed result; fall back to last history item
                val result = scannerState.result ?: historyList.firstOrNull()

                if (result != null) {
                    AnalysisResultScreen(
                        result = result,
                        onBack = { navController.popBackStack() }
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
        // FIX: spec calls for a 0.5dp BorderGrey top divider, NOT Material3's default
        // tonal surface elevation. Draw it manually.
        HorizontalDivider(
            thickness = 0.5.dp,
            color     = BorderGrey     // #E0DFD8
        )

        NavigationBar(
            containerColor  = BackgroundOffWhite,   // #F8F7F2 — matches screen bg
            tonalElevation  = 0.dp                  // removes default M3 tonal shift
        ) {
            screens.forEach { screen ->
                val isSelected = currentDestination
                    ?.hierarchy
                    ?.any { it.route == screen.route } == true

                // FIX: original code computed iconTint outside NavigationBarItem and
                // then also set colors inside it — conflicting definitions.
                // Centralise here with NavigationBarItemDefaults.
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
                            // tint comes from NavigationBarItemDefaults below
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
                        selectedIconColor   = ForestGreen,          // #2D6A4F
                        selectedTextColor   = ForestGreen,
                        unselectedIconColor = Color(0xFFB0AFA8),    // muted
                        unselectedTextColor = Color(0xFFB0AFA8),
                        indicatorColor      = Color.Transparent     // no pill highlight
                    )
                )
            }
        }
    }
}