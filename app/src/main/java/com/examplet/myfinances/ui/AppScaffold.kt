package com.examplet.myfinances.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.examplet.myfinances.ui.components.AppBottomBar
import com.examplet.myfinances.ui.components.AppTopBar
import com.examplet.myfinances.ui.navigation.AppNavHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest: NavDestination? = backStack?.destination

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { AppTopBar() },
        bottomBar = {
            AppBottomBar(
                currentDestination = currentDest,
                onNavigate = { route ->
                    nav.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(nav.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                }
            )
        }
    ) { inner ->
        AppNavHost(
            navController = nav,
            modifier = Modifier.padding(inner)
        )
    }
}
