// MINE: el único sitio que aplica [MapOptions] a un mapa (#16). Lo heredado
// dibuja el mapa con todo fijo; esto lo ajusta a lo que el conductor eligió,
// sin que el archivo heredado sepa de opciones.
package com.openauto.dash.mine

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.openauto.dash.DashColors
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal object MapLook {
    /**
     * Applies the kept options to [map] now and after every change.
     * [wallpaper]: the Canvas backdrop's view, null for the map widget and the
     * cluster, whose camera follows the car on its own.
     */
    fun watch(
        context: Context,
        scope: CoroutineScope,
        map: MapLibreMap,
        wallpaper: MapView?,
        /** The style the map has just loaded: the only one that takes new layers. */
        loaded: Style? = null
    ) {
        MapOptionsStore.load(context)
        followBearing(map)
        MapLayers.watch(context, scope, map, loaded)
        scope.launch {
            MapOptionsStore.current.collect { options ->
                runCatching { apply(map, options) }
            }
        }
    }

    /**
     * The basemap the driver's map is drawn with: the one they chose, or the
     * theme's (#18). Read from the composition, so picking another in the sheet
     * swaps the style while the map is on screen.
     *
     * The Canvas backdrop does not ask here: it follows the theme, as before.
     */
    @Composable
    internal fun mapIsLight(): Boolean {
        MapOptionsStore.load(LocalContext.current)
        val theme = MapOptionsStore.current.collectAsState().value.theme
        return theme.isLight(DashColors.Light)
    }

    fun apply(map: MapLibreMap, options: MapOptions) {
        map.uiSettings.setAllGesturesEnabled(options.gestures)
        // Ours is drawn on top ([MapCompass]): the library's would only show when the map is turned.
        map.uiSettings.isCompassEnabled = false
    }

    /** Maps already listened to: the listener survives recompositions, so it is added once. */
    private val watched = java.util.WeakHashMap<MapLibreMap, Boolean>()

    /** Keeps [MapBearing] on what the camera faces, a degree at a time. */
    private fun followBearing(map: MapLibreMap) {
        if (watched.put(map, true) != null) return
        runCatching {
            map.addOnCameraMoveListener {
                val now = ((map.cameraPosition.bearing.toFloat() % 360f) + 360f) % 360f
                val before = MapBearing.degrees.value
                val step = kotlin.math.abs(now - before).let { kotlin.math.min(it, 360f - it) }
                if (step >= 0.5f) MapBearing.degrees.value = now
            }
        }
    }
}
