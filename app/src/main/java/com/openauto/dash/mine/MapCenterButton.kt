// MINE: el botón de volver al coche del widget de mapa (#17), solo el icono
// (#22). Aparece cuando la cámara está fuera del coche —el conductor movió el
// mapa, o se vio una ruta o una dirección— y al pulsarlo la devuelve al coche
// con el zoom de conducción. Se va solo en cuanto la cámara vuelve a seguir al
// coche. La lógica del estado está en [MapRecentering].
package com.openauto.dash.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
 * The round button that takes [map]'s camera back to the car, shown only while
 * the camera is away from it. Only the arrow, no label (#22): it sits on the map
 * like [MapCompass] and must not hide it. The text survives as the icon's
 * description, so a screen reader still says what it does.
 */
@Composable
internal fun MapCenterButton(map: MapLibreMap?, modifier: Modifier = Modifier, diameter: Dp = 44.dp) {
    val away by MapRecentering.away(map).collectAsState()
    if (!away) return
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(DashColors.Card.copy(alpha = 0.88f))
            .border(1.dp, DashColors.Line, CircleShape)
            .clickable { MapRecentering.centre(map) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.MyLocation,
            contentDescription = stringResource(R.string.mine_map_center),
            tint = DashColors.Accent,
            modifier = Modifier.size(22.dp)
        )
    }
}
