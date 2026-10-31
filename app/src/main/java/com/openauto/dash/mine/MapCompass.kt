// MINE: la brújula del widget de mapa (#16). La de MapLibre solo aparece
// cuando el mapa está girado respecto al norte, así que aparcado no se ve
// nunca; esta se dibuja siempre y gira con el rumbo del mapa.
package com.openauto.dash.mine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openauto.dash.DashColors
import kotlinx.coroutines.flow.MutableStateFlow

/** Where the map faces, in degrees from north: written as the camera turns, read by [MapCompass]. */
internal object MapBearing {
    val degrees = MutableStateFlow(0f)
}

/**
 * The compass, drawn by us: a ring that turns with the map, the north half in
 * the theme's accent. Shown only while the compass option is on.
 */
@Composable
internal fun MapCompass(modifier: Modifier = Modifier, diameter: Dp = 44.dp) {
    MapOptionsStore.current.collectAsState().value.let { options ->
        if (!options.compass) return
        val bearing by MapBearing.degrees.collectAsState()
        val accent = DashColors.Accent
        val muted = DashColors.TextSecondary
        Box(
            modifier = modifier
                .size(diameter)
                .clip(CircleShape)
                .background(DashColors.Card.copy(alpha = 0.72f))
                .border(1.dp, DashColors.Line, CircleShape)
        ) {
            Canvas(Modifier.size(diameter)) {
                val middle = Offset(size.width / 2f, size.height / 2f)
                val north = size.minDimension * 0.34f
                rotate(degrees = -bearing, pivot = middle) {
                    // The needle: north in the accent, the tail muted.
                    drawLine(
                        accent,
                        start = middle,
                        end = Offset(middle.x, middle.y - north),
                        strokeWidth = size.minDimension * 0.09f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        muted,
                        start = middle,
                        end = Offset(middle.x, middle.y + north * 0.75f),
                        strokeWidth = size.minDimension * 0.07f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
