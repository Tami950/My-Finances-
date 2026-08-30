package com.examplet.myfinances.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.examplet.myfinances.R
import com.examplet.myfinances.ui.casa.CasaNavHost

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Dashboard.route,
        modifier = modifier
    ) {
        composable(Route.Dashboard.route) { ScreenPlaceholder(stringResource(R.string.tab_dashboard)) }
        composable(Route.Casa.route) { CasaNavHost() }
        composable(Route.Personale.route) { ScreenPlaceholder(stringResource(R.string.tab_mine)) }
        composable(Route.Bollette.route) { ScreenPlaceholder(stringResource(R.string.tab_bills)) }
    }
}

@Composable
private fun ScreenPlaceholder(title: String) {
    Surface(Modifier.fillMaxSize()) {
        Text(title, Modifier.padding(16.dp))
    }
}
