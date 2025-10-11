package com.examplet.myfinances

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.examplet.myfinances.ui.theme.MyFinancesTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.examplet.myfinances.ui.theme.darkBottomBar
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.examplet.myfinances.ui.theme.white
import com.examplet.myfinances.ui.theme.transparent

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = transparent,   // status bar trasparente in light
                darkScrim  = transparent    // e in dark
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = white,         // nav bar bianca in light
                darkScrim  = darkBottomBar   // nav bar grigio scuro in dark
            )
        )
        super.onCreate(savedInstanceState)
        window.decorView.setOnApplyWindowInsetsListener(null)
        setContent {
            MyFinancesTheme { AppScaffold() }
        }
    }
}

private enum class Route(val route: String, @StringRes val labelRes: Int,) {
    Dashboard("dashboard", R.string.tab_dashboard),
    Casa("casa", R.string.tab_casa),
    Personale("personale", R.string.tab_personale),
    Bollette("bollette", R.string.tab_bollette),
    Settings("settings", R.string.tab_settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest: NavDestination? = backStack?.destination


    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.title_app)) }) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val items = listOf(
                    Route.Dashboard, Route.Casa, Route.Personale, Route.Bollette, Route.Settings
                )
                items.forEach { item ->
                    val selected = currentDest?.hierarchy?.any { it.route == item.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = { if (!selected) nav.navigate(item.route) },
                        // Non usiamo lo slot label: lo gestiamo dentro 'icon'
                        icon = {
                            val iconModifier = Modifier.size(dimensionResource(R.dimen.bottom_nav_icon_size))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Iconcine diverse per tab
                                when (item) {
                                    Route.Dashboard -> Icon(Icons.Outlined.Home, contentDescription = null, iconModifier)
                                    Route.Casa      -> Icon(Icons.Outlined.AccountBalance, contentDescription = null, iconModifier)
                                    Route.Personale -> Icon(Icons.Outlined.AccountCircle, contentDescription = null, iconModifier)
                                    Route.Bollette  -> Icon(Icons.Outlined.ReceiptLong, contentDescription = null, iconModifier)
                                    Route.Settings  -> Icon(Icons.Outlined.Settings, contentDescription = null, iconModifier)
                                }
                                // Testo in una sola riga con ellissi
                                Text(
                                    text = stringResource(item.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                // Barretta di selezione (visibile solo se selected)
                                if (selected) {
                                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_nav_selected_icon_space_from_bottom_size)))
                                    Box(
                                        modifier = Modifier
                                            .width(dimensionResource(R.dimen.bottom_nav_placeholder_width_size))
                                            .height(dimensionResource(R.dimen.bottom_nav_placeholder_height_size))
                                            .clip(RoundedCornerShape(dimensionResource(R.dimen.bottom_nav_placeholder_round_size)))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.bottom_nav_not_selected_icon_space_from_bottom_size)))
                                }
                            }
                        },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Route.Dashboard.route,
            modifier = Modifier
                .padding(inner)
        ) {
            composable(Route.Dashboard.route) { ScreenPlaceholder(stringResource(R.string.tab_dashboard), ) }
            composable(Route.Casa.route) { ScreenPlaceholder(stringResource(R.string.tab_casa)) }
            composable(Route.Personale.route) { ScreenPlaceholder(stringResource(R.string.tab_personale)) }
            composable(Route.Bollette.route) { ScreenPlaceholder(stringResource(R.string.tab_bollette)) }
            composable(Route.Settings.route) { ScreenPlaceholder(stringResource(R.string.tab_settings)) }
        }
    }
}

@Composable
private fun ScreenPlaceholder(title: String) {
    Surface(Modifier.fillMaxSize()) {
        Text(title, Modifier.padding(16.dp))
    }
}