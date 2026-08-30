package com.examplet.myfinances.ui.casa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object CasaRoute {
    const val HOME = "casa/home"
    const val CREATE_PLAN = "casa/create-plan"
    const val EDIT = "casa/edit/{houseMonthId}/{mode}"

    fun edit(houseMonthId: Long, mode: HousePlanEditMode): String =
        "casa/edit/$houseMonthId/${mode.name}"
}

@Composable
fun CasaNavHost() {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        navController.navigate(CasaRoute.HOME) {
            popUpTo(CasaRoute.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = CasaRoute.HOME
    ) {
        composable(CasaRoute.HOME) {
            CasaScreen(
                onCreatePlan = { navController.navigate(CasaRoute.CREATE_PLAN) },
                onEditPlan = { houseMonthId ->
                    navController.navigate(CasaRoute.edit(houseMonthId, HousePlanEditMode.PLAN))
                },
                onEditPositions = { houseMonthId ->
                    navController.navigate(CasaRoute.edit(houseMonthId, HousePlanEditMode.POSITIONS))
                }
            )
        }
        composable(CasaRoute.CREATE_PLAN) {
            CreateHousePlanScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = CasaRoute.EDIT,
            arguments = listOf(
                navArgument("houseMonthId") { type = NavType.LongType },
                navArgument("mode") { type = NavType.StringType }
            )
        ) {
            EditHousePlanScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
