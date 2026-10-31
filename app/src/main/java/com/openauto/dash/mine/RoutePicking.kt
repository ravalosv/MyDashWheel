// MINE: elegir una ruta tocándola (#25). La librería de navegación dice qué
// ruta se tocó por un listener, pero el resto del lienzo no se entera; y al
// pulsar la principal no avisa nada, porque no cambia. Así que además leemos el
// toque aquí —sobre las líneas que nosotros le hemos dado— y escribimos la
// elegida en el mapa con una línea propia encima. Si la librería lo hace a su
// manera, se ve la nuestra; si no, al menos se ve algo.
package com.openauto.dash.mine

import android.util.Log
import com.openauto.dash.Polyline
import org.json.JSONObject
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.navigation.core.models.DirectionsRoute
import java.util.WeakHashMap

internal object RoutePicking {
    private const val SOURCE = "mine-picked-route"
    private const val LAYER = "mine-picked-route-line"

    /** How far a tap may fall from a line and still be that line, in degrees: ~500 m. */
    private const val TOUCH_DEG = 0.005

    /** What the map is showing, by map: the routes and the style to draw on. */
    private class Shown {
        var routes: List<DirectionsRoute> = emptyList()
        var style: Style? = null
    }

    private val shown = WeakHashMap<MapLibreMap, Shown>()
    private val watched = WeakHashMap<MapLibreMap, Boolean>()

    /** The lines just drawn ([routes]): what a tap can pick. */
    fun show(routes: List<DirectionsRoute>) {
        synchronized(this) {
            shown.values.forEach { it.routes = routes }
        }
        Log.i("RoutePicking", "showing ${routes.size} route(s)")
    }

    /**
     * Reads the tap on [map] from now on: [onPick] gets the touched route and how
     * many there were (so the caller can say «there is only one»).
     */
    fun watch(map: MapLibreMap?, onPick: (DirectionsRoute, Int) -> Unit) {
        if (map == null) return
        synchronized(this) {
            if (watched[map] == true) return
            watched[map] = true
        }
        map.addOnMapClickListener { latLng ->
            val (routes, style) = synchronized(this) {
                val s = shown.getOrPut(map) { Shown() }
                s.style = map.style
                s.routes to s.style
            }
            val at = pick(routes, latLng.latitude, latLng.longitude)
            Log.i("RoutePicking", "tap: ${routes.size} route(s), picked ${at?.let { routes.indexOf(it) }}")
            if (at != null) {
                draw(map, style, at)
                onPick(at, routes.size)
            }
            false
        }
    }

    /** The chosen route's line on top of the others, in the accent; none when there is nothing to pick. */
    private fun draw(map: MapLibreMap, style: Style?, route: DirectionsRoute) {
        val s = style ?: map.style ?: return
        runCatching {
            val geoJson = routeGeometry(route) ?: return
            val source = s.getSourceAs(SOURCE) ?: GeoJsonSource(SOURCE).also { s.addSource(it) }
            source.setGeoJson(geoJson)
            if (s.getLayer(LAYER) == null) {
                s.addLayer(
                    LineLayer(LAYER, SOURCE)
                        .withProperties(
                            lineColor("#FF7A1A"),
                            lineWidth(7f),
                            lineOpacity(0.9f),
                            lineCap("round"),
                            lineJoin("round")
                        )
                )
            }
        }.onFailure { Log.w("RoutePicking", "could not draw the picked line", it) }
    }

    /**
     * The route of [lat]/[lng] from the tap's own reading: the nearest line within
     * [TOUCH_DEG]. Null when the tap falls on none of them (or there are none).
     */
    internal fun pick(routes: List<DirectionsRoute>, lat: Double, lng: Double): DirectionsRoute? {
        var best: DirectionsRoute? = null
        var bestDistance = TOUCH_DEG
        for (route in routes) {
            val geometry = route.geometry ?: continue
            val points = runCatching { Polyline.decode(geometry, 6) }.getOrNull() ?: continue
            for (i in 0 until points.size - 1) {
                val d = segmentDistance(lat, lng, points[i], points[i + 1])
                if (d < bestDistance) {
                    bestDistance = d
                    best = route
                }
            }
        }
        return best
    }

    /** Shortest distance, in degrees, from (lat, lng) to the segment a–b (points are (lat, lon)). */
    internal fun segmentDistance(
        lat: Double, lng: Double,
        a: Pair<Double, Double>, b: Pair<Double, Double>
    ): Double {
        // A plane is enough at tap scale: latitude corrects the longitude's stretch.
        val cos = kotlin.math.cos(Math.toRadians(lat))
        val ax = (a.second - lng) * cos
        val ay = a.first - lat
        val bx = (b.second - lng) * cos
        val by = b.first - lat
        val dx = bx - ax
        val dy = by - ay
        val length = dx * dx + dy * dy
        val t = if (length <= 0.0) 0.0 else ((-ax * dx) + (-ay * dy)) / length
        val clamped = t.coerceIn(0.0, 1.0)
        val px = ax + clamped * dx
        val py = ay + clamped * dy
        return kotlin.math.hypot(px, py)
    }

    /** [route]'s geometry as a one-feature GeoJSON, to hand to a source. */
    internal fun routeGeometry(route: DirectionsRoute): String? {
        val geometry = route.geometry ?: return null
        return JSONObject()
            .put("type", "Feature")
            .put("properties", JSONObject())
            .put("geometry", JSONObject(geometry))
            .toString()
    }
}
