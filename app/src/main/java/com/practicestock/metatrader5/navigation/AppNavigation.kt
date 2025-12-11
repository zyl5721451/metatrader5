package com.practicestock.metatrader5.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.practicestock.metatrader5.data.AppPreferences
import com.practicestock.metatrader5.ui.screens.CalculatorScreen
import com.practicestock.metatrader5.ui.screens.SettingsScreen

// 定义路由
object Routes {
    const val CALCULATOR = "calculator"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(navController: NavHostController, appPreferences: AppPreferences) {
    NavHost(navController = navController, startDestination = Routes.CALCULATOR) {
        composable(Routes.CALCULATOR) {
            CalculatorScreen(
                appPreferences = appPreferences,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                appPreferences = appPreferences,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
