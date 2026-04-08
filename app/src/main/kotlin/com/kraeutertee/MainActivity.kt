@file:OptIn(ExperimentalMaterial3Api::class)

package com.kraeutertee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.kraeutertee.ui.navigation.Routes
import com.kraeutertee.ui.navigation.Screen
import com.kraeutertee.ui.screens.*
import com.kraeutertee.ui.theme.KraeuterTeeTheme
import com.kraeutertee.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KraeuterTeeTheme {
                KraeuterTeeApp(application)
            }
        }
    }
}

@Composable
fun KraeuterTeeApp(app: android.app.Application) {
    val factory      = remember { AppViewModelFactory(app) }
    val navController = rememberNavController()

    val herbsVm:    HerbsViewModel       = viewModel(factory = factory)
    val customVm:   CustomHerbsViewModel = viewModel(factory = factory)
    val mapVm:      MapViewModel         = viewModel(factory = factory)
    val calVm:      CalendarViewModel    = viewModel(factory = factory)
    val recipesVm:  RecipesViewModel     = viewModel(factory = factory)
    val kiVm:       KIViewModel          = viewModel(factory = factory)
    val settingsVm: SettingsViewModel    = viewModel(factory = factory)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = Screen.bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon     = { Icon(screen.icon, contentDescription = screen.titleDE) },
                            label    = { Text(screen.titleDE) },
                            selected = currentRoute == screen.route,
                            onClick  = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        )
                    }
                }
            }
        },
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = {
                        Text(Screen.bottomNavItems.find { it.route == currentRoute }?.titleDE ?: "Kräutertee")
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                            Icon(Icons.Default.Settings, "Einstellungen")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Kraeuter.route,
            modifier         = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Kraeuter.route) {
                HerbsScreen(
                    vm       = herbsVm,
                    customVm = customVm,
                    onNavigateToDetail = { herbId ->
                        navController.navigate(Routes.herbDetail(herbId))
                    }
                )
            }
            composable(Routes.HERB_DETAIL) { backStack ->
                val herbId = backStack.arguments?.getString("herbId") ?: return@composable
                HerbDetailScreen(
                    herbId   = herbId,
                    vm       = herbsVm,
                    customVm = customVm,
                    onBack   = { navController.popBackStack() }
                )
            }
            composable(Screen.Karte.route)    { MapScreen(vm = mapVm) }
            composable(Screen.Kalender.route) { CalendarScreen(vm = calVm) }
            composable(Screen.Rezepte.route)  { RecipesScreen(vm = recipesVm) }
            composable(Screen.KI.route)       { KIScreen(vm = kiVm) }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm = settingsVm, onBack = { navController.popBackStack() })
            }
        }
    }
}
