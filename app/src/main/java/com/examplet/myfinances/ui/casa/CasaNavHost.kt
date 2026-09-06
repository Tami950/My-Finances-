package com.examplet.myfinances.ui.casa

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object CasaRoute {
    const val HOME = "casa/home"
    const val CREATE_PLAN = "casa/create-plan"
    const val EDIT = "casa/edit/{houseMonthId}/{mode}"
    const val CLOSE_MONTH = "casa/close/{houseMonthId}"

    fun edit(houseMonthId: Long, mode: HousePlanEditMode): String =
        "casa/edit/$houseMonthId/${mode.name}"

    fun closeMonth(houseMonthId: Long): String = "casa/close/$houseMonthId"
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
                onCreatePlan = { navController.navigate(CasaRoute.CREATE_PLAN) },
                onEditPlan = { houseMonthId ->
                    navController.navigate(CasaRoute.edit(houseMonthId, HousePlanEditMode.PLAN))
                },
                onEditPositions = { houseMonthId ->
                    navController.navigate(CasaRoute.edit(houseMonthId, HousePlanEditMode.POSITIONS))
                },
                onCloseMonth = { houseMonthId ->
                    navController.navigate(CasaRoute.closeMonth(houseMonthId))
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
        composable(
            route = CasaRoute.CLOSE_MONTH,
            arguments = listOf(
                navArgument("houseMonthId") { type = NavType.LongType }
            )
        ) {
            CloseHouseMonthScreen(
                onBack = { navController.popBackStack() },
                onClosed = { navController.popBackStack() }
            )
        }
    }
}
