package com.timalo.mobileevent.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.model.Environment
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
    env: Environment,
    initialToken: String?
) {
    val navController = rememberNavController()

    // Réagit à la perte de token de l'env courant (déconnexion) pour repartir au login.
    val token by prefs.tokenFlow(env).collectAsState(initial = initialToken)

    val startDestination = if (initialToken.isNullOrBlank()) Routes.LOGIN else Routes.CREATE_EVENT

    NavHost(navController = navController, startDestination = startDestination) {

        composable(
            route = "${Routes.LOGIN}?env={env}",
            arguments = listOf(navArgument("env") { type = NavType.StringType; defaultValue = "" })
        ) { backStack ->
            val vm: LoginViewModel = viewModel(factory = factory)
            val envParam = backStack.arguments?.getString("env").orEmpty()
            if (envParam.isNotBlank()) vm.forceEnv(Environment.fromName(envParam))
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
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onNeedsLogin = { targetEnv ->
                    // Sauvegarde l'env cible et navigue vers le login
                    navController.navigate("${Routes.LOGIN}?env=${targetEnv.name}") {
                        popUpTo(Routes.CREATE_EVENT)
                    }
                }
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
