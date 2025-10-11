package com.examplet.myfinances

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.examplet.myfinances.ui.theme.MyFinancesTheme
import com.examplet.myfinances.ui.AppScaffold
import com.examplet.myfinances.ui.theme.darkBottomBar
import com.examplet.myfinances.ui.theme.transparent
import com.examplet.myfinances.ui.theme.white

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = transparent,
                darkScrim  = transparent
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = white,
                darkScrim  = darkBottomBar
            )
        )
        super.onCreate(savedInstanceState)
        setContent { MyFinancesTheme { AppScaffold() } }
    }
}
