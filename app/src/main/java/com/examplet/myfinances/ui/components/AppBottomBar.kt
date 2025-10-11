package com.examplet.myfinances.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.examplet.myfinances.ui.navigation.BottomBarItems
import com.examplet.myfinances.R


@Composable
fun AppBottomBar(
    currentDestination: NavDestination?,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    dividerColor: Color = MaterialTheme.colorScheme.outlineVariant,
    onNavigate: (String) -> Unit
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val barHeight = if (isLandscape) 58.dp else 60.dp

    Surface(
        tonalElevation = NavigationBarDefaults.Elevation,
        color = containerColor,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            // Padding SOLO in basso per nav/gesture bar
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
    ) {
        HorizontalDivider(thickness = 1.dp, color = dividerColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight), // <— altezza fissa SOLO qui
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        )  {
            BottomBarItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                BottomBarItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(item.labelRes),
                    selected = selected,
                    icon = item.icon
                ){ if (!selected) onNavigate(item.route) }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val iconSize = dimensionResource(R.dimen.bottom_nav_icon_size)

    // colori coerenti con NavigationBarItemDefaults
    val iconColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxHeight()
            .minimumInteractiveComponentSize() // ≥48dp touch target
            .clickable(
                interactionSource = interaction,
                indication = null, // usa ripple di default del tema M3
                onClick = onClick,
                role = Role.Tab
            ),
        contentAlignment = Alignment.Center
    ){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = iconColor
            )
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
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
}
