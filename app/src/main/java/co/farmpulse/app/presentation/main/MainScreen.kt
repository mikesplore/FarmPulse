package co.farmpulse.app.presentation.main

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.vector.ImageVector
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
import co.farmpulse.app.ui.theme.ForestGreen
import co.farmpulse.app.ui.theme.BackgroundOffWhite
import co.farmpulse.app.ui.theme.BorderGrey
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Outlined.Home)
    object Forecast : Screen("forecast", "Forecast", Icons.Outlined.CalendarMonth)
    object Scanner : Screen("scanner", "Scanner", Icons.Outlined.PhotoCamera)
    object History : Screen("history", "History", Icons.Outlined.History)
}

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    forecastViewModel: ForecastViewModel,
    scannerViewModel: ScannerViewModel,
    historyViewModel: HistoryViewModel
) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Forecast, Screen.Scanner, Screen.History)

    Scaffold(
        bottomBar = {
            Column {
                Divider(color = BorderGrey, modifier = Modifier.fillMaxWidth().height(0.5.dp))
                NavigationBar(
                    containerColor = BackgroundOffWhite,
                    tonalElevation = 0.dp
                ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val iconTint = if (isSelected) ForestGreen else Color(0xFFB0AFA8)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label, modifier = Modifier.size(20.dp), tint = iconTint) },
                        label = {
                            Text(
                                screen.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = iconTint
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            unselectedIconColor = Color(0xFFB0AFA8),
                            indicatorColor = Color.Transparent
                        )
                    )
                }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(100)) }
        ) {
            composable(Screen.Home.route) { HomeScreen(homeViewModel) }
            composable(Screen.Forecast.route) { ForecastScreen(forecastViewModel) }
            composable(Screen.Scanner.route) {
                ScannerScreen(scannerViewModel) { navController.navigate("scanner/result") }
            }
            composable(Screen.History.route) { 
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToResult = { /* Pass specific result (handled by screen/viewmodel) */ navController.navigate("scanner/result") },
                    onNewScan = { navController.navigate(Screen.Scanner.route) }
                ) 
            }
            composable(
                "scanner/result",
                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
            ) {
                // For demonstration, use the last result from scanner or a default
                val scannerState by scannerViewModel.uiState.collectAsState()
                val historyList by historyViewModel.history.collectAsState()

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
