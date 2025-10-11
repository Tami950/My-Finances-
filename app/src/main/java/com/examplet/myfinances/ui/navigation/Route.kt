package com.examplet.myfinances.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector
import com.examplet.myfinances.R

enum class Route(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    Dashboard("dashboard", R.string.tab_dashboard, Icons.Outlined.Home),
    Casa("casa", R.string.tab_home, Icons.Outlined.AccountBalance),
    Personale("personale", R.string.tab_mine, Icons.Outlined.AccountCircle),
    Bollette("bollette", R.string.tab_bills, Icons.AutoMirrored.Outlined.ReceiptLong)
}

val BottomBarItems = listOf(
    Route.Dashboard, Route.Casa, Route.Personale, Route.Bollette
)
