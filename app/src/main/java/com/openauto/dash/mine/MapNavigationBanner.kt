// MINE: #30 show Google Maps and Waze's current maneuver on the map widget.
package com.openauto.dash.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.openauto.dash.DashColors
import com.openauto.dash.DashSize
import com.openauto.dash.DashShape
import com.openauto.dash.ManeuverIcon
import com.openauto.dash.NavDirections
import com.openauto.dash.NavHandoff
import com.openauto.dash.NavState
import com.openauto.dash.GOOGLE_MAPS_PACKAGE
import com.openauto.dash.navAppName
import com.openauto.dash.R
import com.openauto.dash.runningNavApp
import com.openauto.dash.stopExternalNavigation
import androidx.compose.ui.platform.LocalContext

internal fun mapHasExternalNavigation(state: NavState, hasHandedRoute: Boolean): Boolean =
    hasHandedRoute || (state.active && state.packageName in NavDirections.PACKAGES)

@Composable
internal fun MapDestinationControls(content: @Composable () -> Unit) {
    val state by NavDirections.state.collectAsState()
    val handedRoute by RouteFeed.handedRoute.collectAsState()
    if (!mapHasExternalNavigation(state, handedRoute != null)) content()
}

internal fun mapBannerNavigation(
    state: NavState,
    hasHandedRoute: Boolean,
    runningApp: String?,
    preferredApp: String?,
    continueLabel: String
): NavState? {
    if (state.active && state.packageName in NavDirections.PACKAGES) return state
    if (!hasHandedRoute) return null
    val app = listOfNotNull(runningApp, preferredApp, GOOGLE_MAPS_PACKAGE)
        .firstOrNull { it in NavDirections.PACKAGES } ?: return null
    return NavState(active = true, instruction = continueLabel, packageName = app)
}

@Composable
internal fun MapNavigationBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by NavDirections.state.collectAsState()
    val handedRoute by RouteFeed.handedRoute.collectAsState()
    val runningApp = runningNavApp()
    val nav = mapBannerNavigation(
        state = state,
        hasHandedRoute = handedRoute != null,
        runningApp = runningApp,
        preferredApp = NavHandoff.preferredApp(context),
        continueLabel = stringResource(R.string.info_nav_continue)
    ) ?: return
    val (distance, unit) = nav.distanceParts
    val eta = nav.etaParts.joinToString(" · ")

    Surface(
        color = DashColors.Card.copy(alpha = 0.96f),
        shape = DashShape.Medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ManeuverIcon(nav, 44.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = navAppName(nav.packageName),
                        color = DashColors.AccentInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (distance.isNotEmpty()) {
                        Text(
                            text = "$distance $unit",
                            color = DashColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    if (eta.isNotEmpty()) {
                        Text(
                            text = eta,
                            color = DashColors.TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = nav.instruction,
                    color = DashColors.TextPrimary,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = { stopExternalNavigation(context, nav) },
                modifier = Modifier.size(DashSize.TouchPrimary)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.info_map_stop),
                    tint = DashColors.TextSecondary
                )
            }
        }
    }
}
