package com.timalo.mobileevent.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.ui.screens.CreateEventScreen
import com.timalo.mobileevent.ui.screens.LoginScreen
import com.timalo.mobileevent.ui.screens.SettingsScreen
import com.timalo.mobileevent.viewmodel.CreateEventViewModel
import com.timalo.mobileevent.viewmodel.LoginViewModel

object Routes {
    const val LOGIN = "login"
    const val CREATE_EVENT = "create_event"
    const val SETTINGS = "settings"
}

/**
 * Navigation principale. Le graphe de départ dépend du token :
 * - pas de token → Login
 * - token présent → Création d'événement
 */
@Composable
fun AppNavigation(
    factory: ViewModelProvider.Factory,
    prefs: AppPreferences,
    initialToken: String?
) {
    val navController = rememberNavController()

    // Réagit à la perte de token (déconnexion) pour repartir au login.
    val token by prefs.tokenFlow.collectAsState(initial = initialToken)

    val startDestination = if (initialToken.isNullOrBlank()) Routes.LOGIN else Routes.CREATE_EVENT

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = vm,
                onLoggedIn = {
                    navController.navigate(Routes.CREATE_EVENT) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CREATE_EVENT) {
            val vm: CreateEventViewModel = viewModel(factory = factory)
            CreateEventScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
