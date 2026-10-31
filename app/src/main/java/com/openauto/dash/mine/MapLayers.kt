// MINE: las capas que añadimos al mapa del widget (#16). MapLibre 11 no avisa
// cuando el estilo termina de cargarse y el mapa cambia de estilo al pasar de
// día a noche: aquí se viste el estilo que se acaba de cargar y se vigila cuál
// está puesto, por si el mapa lo cambia sin avisar. Son dos: los incidentes de
// TomTom ([MapTraffic]), que se ven siempre, y el tráfico de la ruta trazada
// ([MapRouteTraffic]), que solo existe con ruta.
package com.openauto.dash.mine

import android.content.Context
import android.util.Log
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object MapLayers {
    /** How often a style is looked at while it is not the one already dressed. */
    private const val TICK_MS = 1000L

    /** What a map was last dressed with: only recorded once the style really took it. */
    private class Dressed(
        val style: Style,
        /** The traffic option: it is what the incidents hang on, route or no route. */
        val traffic: Boolean,
        val key: String,
        val bands: List<RouteBand>
    )

    private val dressed = java.util.WeakHashMap<MapLibreMap, Dressed>()

    /**
     * Watches [map] from now on; call once per map. [loaded]: the style the
     * map has just finished loading, which is the one that accepts layers.
     */
    fun watch(context: Context, scope: CoroutineScope, map: MapLibreMap, loaded: Style? = null) {
        // The route, and the traffic on it, are asked for once for the app, not once per map.
        MapRouteTraffic.start(context)
        scope.launch {
            var first = loaded
            while (true) {
                try {
                    dress(context, map, first)
                    first = null
                } catch (t: Throwable) {
                    Log.w("MapTraffic", "map layers threw: ${t.javaClass.simpleName}: ${t.message}")
                }
                delay(TICK_MS)
            }
        }
    }

    private fun dress(context: Context, map: MapLibreMap, loaded: Style?) {
        val style = loaded ?: map.style ?: return
        val options = MapOptionsStore.current.value
        val key = MapTraffic.key(context)
        // Incidents always, route or no route (the owner's call): a crash on the road ahead is worth
        // seeing even when nothing is being guided. The colours of the route are the ones that need it.
        val traffic = options.traffic
        val jams = if (traffic) MapRouteTraffic.jams.value else null
        val bands = jams?.bands.orEmpty()
        // The library's route line is redrawn where it likes; the colours stay over it whatever it does.
        MapRouteTraffic.onTop(style)
        val before = dressed[map]
        if (before != null && before.style === style && before.traffic == traffic &&
            before.key == key && before.bands === bands
        ) return
        // Recorded only once the style kept it: a style that is not the live one is tried again.
        val trafficKept = MapTraffic.apply(style, traffic, key)
        val bandsKept = MapRouteTraffic.apply(style, bands)
        if (trafficKept && bandsKept) dressed[map] = Dressed(style, traffic, key, bands)
    }
}
