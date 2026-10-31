// MINE: los incidentes de TomTom sobre el mapa (#16): la capa raster que trae
// obras, cortes y accidentes, con la clave del conductor. Se pinta siempre que
// haya clave y la opción de tráfico esté encendida, con ruta o sin ella: un
// accidente delante del coche se quiere ver aunque no se vaya a ningún sitio
// (el flujo de la ciudad sí necesita ruta, y vive en [MapRouteTraffic]). Nada
// de esto vive en el archivo heredado: lo llama [MapLayers].
package com.openauto.dash.mine

import android.content.Context
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.maps.Style

internal object MapTraffic {
    private const val PREFS = "map_traffic"
    private const val KEY = "tomtom_key"

    /** Where a driver gets a free key (200K requests a month cover a car). */
    const val SIGN_UP = "https://developer.tomtom.com/"

    private const val INCIDENTS = "mine-traffic-incidents"
    private const val INCIDENTS_LAYER = "mine-traffic-incidents-layer"

    /** The key kept, blank for none. */
    fun key(context: Context): String =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "").orEmpty().trim()

    /** Keeps [value]; blank removes it and, with it, the tiles behind it. */
    fun setKey(context: Context, value: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, value.trim()).apply()
    }

    /**
     * Puts TomTom's incident tiles over [style], or takes them away when [on]
     * is off or there is no key. Below the first label layer, so the street
     * names stay readable over them.
     *
     * [on] is the traffic option alone, not a route: the incidents —crashes,
     * roadworks, closures— are worth seeing on the road ahead whether or not a
     * route is traced. It is the city's flow that is not asked for without one
     * ([MapRouteTraffic]).
     *
     * Returns whether [style] ended up as asked: MapLibre ignores a source
     * added to a style that is not the live one without saying so, and the
     * caller must then try again rather than believe it was done.
     */
    fun apply(style: Style, on: Boolean, key: String): Boolean {
        if (!on || key.isBlank()) {
            remove(style)
            return style.getSource(INCIDENTS) == null
        }
        add(style, INCIDENTS, INCIDENTS_LAYER, "incidents/s3", key, opacity = 0.9f)
        return style.getSource(INCIDENTS) != null
    }

    private fun add(style: Style, id: String, layerId: String, path: String, key: String, opacity: Float) {
        if (style.getSource(id) != null) return
        val url = "https://api.tomtom.com/traffic/map/4/tile/$path/{z}/{x}/{y}.png?key=$key"
        val source = RasterSource(id, TileSet("tileset", url), 256)
        style.addSource(source)
        val layer = RasterLayer(layerId, id).withProperties(rasterOpacity(opacity))
        val firstLabel = style.layers.firstOrNull { it is SymbolLayer }?.id
        if (firstLabel != null) style.addLayerBelow(layer, firstLabel) else style.addLayer(layer)
    }

    /** Its layer first, or MapLibre refuses to drop a source still in use. */
    private fun remove(style: Style) {
        if (style.getLayer(INCIDENTS_LAYER) != null) style.removeLayer(INCIDENTS_LAYER)
        if (style.getSource(INCIDENTS) != null) style.removeSource(INCIDENTS)
    }
}
