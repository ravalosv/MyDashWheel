// MINE: el tráfico de la ruta trazada (#16). Con una ruta puesta, el color va
// sobre la línea que el mapa ya dibuja —como en Google Maps— y no sobre toda la
// ciudad: se le pide a TomTom su ruta con tráfico y sus tramos se pegan a la
// nuestra ([RouteBands]). Sin ruta no se pinta nada aquí: manda el flujo de
// [MapTraffic], que cubre las calles alrededor del coche.
package com.openauto.dash.mine

import android.content.Context
import android.util.Log
import com.openauto.dash.Http
import com.openauto.dash.OVERPASS_AGENT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import okhttp3.Request
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

internal object MapRouteTraffic {
    const val TAG = "MapTraffic"

    /** The same route is asked again this often: traffic moves, the line does not. */
    private const val REFRESH_MS = 3 * 60_000L

    /** After a refused or lost request: sooner than a route's own refresh, which is minutes away. */
    private const val RETRY_MS = 20_000L

    private const val API = "https://api.tomtom.com/routing/1/calculateRoute"

    /** As wide as the route line it is drawn over, so the colour is the line's. */
    private const val WIDTH = 10f

    /** The route line's own layer, as the navigation library names it: our colours go over it. */
    private const val ROUTE_LAYER = "mapbox-navigation-route-layer"

    /** One delay step TomTom reports, with its colour and the source and layer it is painted in. */
    private class Step(val delay: Int, val colour: Int) {
        val source = "mine-route-traffic-$delay"
        val layer = "mine-route-traffic-$delay-layer"
    }

    private val steps = listOf(
        Step(1, 0xFFF5C443.toInt()),  // minor: amber
        Step(2, 0xFFF28C28.toInt()),  // moderate: orange
        Step(3, 0xFFD93025.toInt()),  // major: red
        Step(4, 0xFF9C1B1B.toInt())   // blocked: dark red
    )

    /** The jams of one route: [key] says which, and an empty list means the road is clear. */
    internal class Jams(val key: String, val bands: List<RouteBand>)

    private val _jams = MutableStateFlow<Jams?>(null)

    /**
     * The congestion of the route being drawn; null when there is no route, no
     * key, or TomTom has not answered yet. Null is what lets the city-wide flow
     * of [MapTraffic] back in: a route with nothing on it leaves the map clean.
     */
    val jams: StateFlow<Jams?> = _jams

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var started = false

    /** Watches the route and asks TomTom for it while the traffic is on; call once per process. */
    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        appContext = context.applicationContext
        RouteFeed.start()
        scope.launch {
            combine(MapOptionsStore.current, RouteFeed.route) { options, route ->
                if (options.traffic) route else null
            }
                .distinctUntilChanged { old, new -> old?.key == new?.key }
                .collectLatest { follow(it) }
        }
    }

    private suspend fun follow(line: RouteLine?) {
        val context = appContext ?: return
        if (line == null) {
            _jams.value = null
            return
        }
        // Colours of the line before are of another road: gone until this one answers.
        if (_jams.value?.key != line.key) _jams.value = null
        while (true) {
            val key = MapTraffic.key(context)
            if (key.isBlank()) _jams.value = null
            val bands = if (key.isBlank()) null else runCatching { fetch(line, key) }.getOrNull()
            if (bands != null) {
                _jams.value = Jams(line.key, bands)
                Log.i(TAG, "route traffic: ${bands.size} jam(s) over ${line.points.size} points")
            }
            // No answer (no key, offline, refused): asked again soon, not a route's refresh away.
            delay(if (bands == null) RETRY_MS else REFRESH_MS)
        }
    }

    /** TomTom's own route from the same ends, with its congested stretches, worn onto [line]. */
    private fun fetch(line: RouteLine, key: String): List<RouteBand>? {
        val from = line.points.first()
        val to = line.points.last()
        // No instructionsType: TomTom only takes coded/text/tagged and answers 400 to anything else.
        val url = "$API/${from.first},${from.second}:${to.first},${to.second}/json" +
            "?key=$key&traffic=true&sectionType=traffic&travelMode=car&routeRepresentation=polyline"
        val request = Request.Builder().url(url).header("User-Agent", OVERPASS_AGENT).build()
        Http.client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                // The body says which product the key is missing (a TomTom key is enabled per API).
                Log.w(TAG, "route traffic: HTTP ${resp.code} ${resp.body?.string().orEmpty().take(300)}")
                return null
            }
            val body = resp.body?.string().orEmpty()
            val bands = RouteBands.parse(body, line.points) ?: return null
            if (bands.isEmpty()) {
                // Tells a clear road from a route TomTom walked somewhere else: the answer has the jams or not.
                val found = Regex("\"sectionType\"\\s*:\\s*\"TRAFFIC\"", RegexOption.IGNORE_CASE).findAll(body).count()
            }
            return bands
        }
    }

    /**
     * Paints [bands] over [style], one layer per delay step, above the route
     * line so the congested stretches take its colour. Empty [bands]: the
     * layers go, and the city-wide flow can show instead.
     *
     * Returns whether the style ended up as asked (a style that is not the live
     * one takes nothing without saying so).
     */
    fun apply(style: Style, bands: List<RouteBand>): Boolean {
        if (bands.isEmpty()) {
            remove(style)
            return steps.none { style.getSource(it.source) != null }
        }
        val used = bands.map { it.delay }.toSet()
        for (step in steps) {
            if (step.delay !in used) {
                remove(style, step)
                continue
            }
            val drawn = FeatureCollection.fromFeatures(
                bands.filter { it.delay == step.delay }.map { band ->
                    Feature.fromGeometry(
                        LineString.fromLngLats(band.points.map { Point.fromLngLat(it.second, it.first) })
                    )
                }
            )
            val source = style.getSource(step.source) as? GeoJsonSource
            if (source != null) {
                source.setGeoJson(drawn)
            } else {
                style.addSource(GeoJsonSource(step.source, drawn))
                place(
                    style,
                    LineLayer(step.layer, step.source).withProperties(
                        lineColor(step.colour),
                        lineWidth(WIDTH),
                        lineCap(Property.LINE_CAP_ROUND),
                        lineJoin(Property.LINE_JOIN_ROUND),
                        lineOpacity(0.92f)
                    )
                )
            }
        }
        return steps.filter { it.delay in used }.all { style.getSource(it.source) != null }
    }

    /** Right over the route line, and under the turn arrow the library draws above it. */
    private fun place(style: Style, layer: LineLayer) {
        if (style.getLayer(ROUTE_LAYER) != null) style.addLayerAbove(layer, ROUTE_LAYER) else style.addLayer(layer)
    }

    /**
     * Keeps the colours over the route line. The library draws its line where
     * and when it likes —on a new route, after a style change— and a jam
     * underneath it would never be seen.
     */
    fun onTop(style: Style) {
        val route = style.layers.indexOfFirst { it.id == ROUTE_LAYER }
        if (route < 0) return
        for (step in steps) {
            val at = style.layers.indexOfFirst { it.id == step.layer }
            if (at < 0 || at > route) continue
            runCatching { style.getLayer(step.layer)?.let { style.addLayerAbove(it, ROUTE_LAYER) } }
        }
    }

    private fun remove(style: Style) {
        steps.forEach { remove(style, it) }
    }

    /** One step's layer away first: MapLibre refuses to drop a source still in use. */
    private fun remove(style: Style, step: Step) {
        if (style.getLayer(step.layer) != null) style.removeLayer(step.layer)
        if (style.getSource(step.source) != null) style.removeSource(step.source)
    }
}
