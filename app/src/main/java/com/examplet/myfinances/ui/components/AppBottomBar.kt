package com.examplet.myfinances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.examplet.myfinances.R
import com.examplet.myfinances.ui.navigation.BottomBarItems

@Composable
fun AppBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        BottomBarItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(item.route) },
                alwaysShowLabel = false,
                icon = {
                    val iconSize = dimensionResource(R.dimen.bottom_nav_icon_size)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(item.icon, contentDescription = null, modifier = Modifier.size(iconSize))
                        Text(
                            text = stringResource(item.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (selected) {
                            Spacer(Modifier.height(dimensionResource(R.dimen.bottom_nav_selected_icon_space_from_bottom_size)))
                            Box(
                                Modifier
                                    .width(dimensionResource(R.dimen.bottom_nav_placeholder_width_size))
                                    .height(dimensionResource(R.dimen.bottom_nav_placeholder_height_size))
                                    .clip(RoundedCornerShape(dimensionResource(R.dimen.bottom_nav_placeholder_round_size)))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            Spacer(Modifier.height(dimensionResource(R.dimen.bottom_nav_not_selected_icon_space_from_bottom_size)))
                        }
                    }
                }
            )
        }
    }
}
