// MINE: el botón «Ruta completa» del widget de mapa (#23). Solo aparece con una
// ruta en curso —la que el mapa está dibujando— y al pulsarlo encuadra toda la
// línea. La cámara deja de seguir al coche, así que el botón de «Centrar»
// (#17) aparece para volver.
package com.openauto.dash.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openauto.dash.DashColors
import com.openauto.dash.R
import org.maplibre.android.maps.MapLibreMap

/**
 * The round button that puts the whole of [map]'s route on the screen, shown
 * only while there is a route to frame. Only the icon, like the button that
 * takes the camera back to the car (#22); its label is the icon's description.
 */
@Composable
internal fun MapRouteOverviewButton(map: MapLibreMap?, modifier: Modifier = Modifier, diameter: Dp = 44.dp) {
    if (map == null) return
    // The feed is the same one the traffic paints on (#16); starting it twice is harmless.
    RouteFeed.start()
    val route by RouteFeed.route.collectAsState()
    val line = route ?: return
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(DashColors.Card.copy(alpha = 0.88f))
            .border(1.dp, DashColors.Line, CircleShape)
            .clickable { MapRouteOverview.fit(map, line) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Route,
            contentDescription = stringResource(R.string.mine_route_overview),
            tint = DashColors.Accent,
            modifier = Modifier.size(22.dp)
        )
    }
}
