package com.examplet.myfinances.ui.casa

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object CasaRoute {
    const val HOME = "casa/home"
    const val CREATE_PLAN = "casa/create-plan"
}

@Composable
fun CasaNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = CasaRoute.HOME
    ) {
        composable(CasaRoute.HOME) {
            CasaScreen(
                onCreatePlan = { navController.navigate(CasaRoute.CREATE_PLAN) }
            )
        }
        composable(CasaRoute.CREATE_PLAN) {
            CreateHousePlanScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
