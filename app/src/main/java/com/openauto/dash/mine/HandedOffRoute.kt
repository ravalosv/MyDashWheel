// MINE: destino entregado a Maps o Waze, dibujado en el mapa propio (#13).
// Todo lo nuevo vive aquí; el código heredado solo llama a HandedOffRouteOnMap.
package com.openauto.dash.mine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.openauto.dash.InAppNav
import com.openauto.dash.LocationFeed
import com.openauto.dash.NavHandoff
import com.openauto.dash.PlaceSearch
import com.openauto.dash.Polyline
import com.openauto.dash.RouteProgress
import com.openauto.dash.valhallaRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.Point
import org.maplibre.navigation.android.navigation.ui.v5.route.NavigationMapRoute
import org.maplibre.navigation.core.models.DirectionsRoute

/**
 * The destination handed to Maps or Waze, drawn on Dashwheel's own map: the map
 * tile's and the Canvas theme's wallpaper, which otherwise only ever draw the
 * in-app guidance's route and so go blank the moment a handoff starts one
 * (#13). The line is Dashwheel's own, from the car by the same free router the
 * rest of the app uses, and it is asked again when the car leaves it: it shows
 * where the trip goes, not the turn the other app is giving.
 */
internal object HandedOffRoute {
    /** What this fork's own log lines are tagged with, one tag to read on the unit. */
    const val TAG = "HandedOffRoute"

    /** A handed-off destination still worth drawing this long: a trip, not the one from yesterday. */
    const val MAX_AGE_MS = 4 * 60 * 60_000L

    /** This close to the destination, and this near the line, the trip is over. */
    const val ARRIVED_M = 80.0

    /** The car this far off the line, for this long, took another road: the line is asked again. */
    const val OFF_ROUTE_M = 100.0
    const val OFF_ROUTE_MS = 10_000L

    /** The least time between two route requests. */
    private const val REROUTE_GAP_MS = 45_000L
    /** Offline or a failing router: as often as this, not more. */
    private const val REROUTE_GAP_MAX_MS = 3 * 60_000L

    /** How often the position is looked at while a line is drawn. */
    const val TICK_MS = 2_000L

    /**
     * What the map should draw: [target] while the in-app guidance is not
     * running (it owns the line when it is), while the destination can still be
     * placed (a position, or the words to look one up) and while it is not older
     * than [MAX_AGE_MS]. Null: nothing.
     */
    fun shown(target: NavHandoff.Target?, guidingInApp: Boolean, nowMs: Long): NavHandoff.Target? {
        if (target == null || guidingInApp) return null
        if (nowMs - target.at > MAX_AGE_MS) return null
        val hasPosition = target.lat != null && target.lng != null
        if (!hasPosition && target.query.isNullOrBlank()) return null
        return target
    }

    /** The gap between two route requests after [failures] in a row: the more of them, the longer. */
    fun gap(failures: Int): Long =
        (REROUTE_GAP_MS shl failures.coerceIn(0, 4)).coerceAtMost(REROUTE_GAP_MAX_MS)

    /** The destination's position: given, or looked up from its words, near the car. Blocking. */
    suspend fun resolve(target: NavHandoff.Target): Point? = withContext(Dispatchers.IO) {
        val lat = target.lat
        val lng = target.lng
        if (lat != null && lng != null) return@withContext Point.fromLngLat(lng, lat)
        val query = target.query?.takeIf { it.isNotBlank() } ?: return@withContext null
        val here = LocationFeed.location.value
        runCatching { PlaceSearch.find(query, here?.latitude, here?.longitude) }.getOrNull()
            ?.let { Point.fromLngLat(it.lng, it.lat) }
    }
}

/**
 * Draws [HandedOffRoute] on the map [map] already has, along the same route
 * line its own guidance uses ([navRoute]): the destination's marker and the
 * line from the car, and nothing at all while the in-app guidance is the one
 * guiding. As a wallpaper (the Canvas theme) it is the only line there is.
 */
@Composable
internal fun HandedOffRouteOnMap(map: MapLibreMap?, navRoute: NavigationMapRoute?) {
    val context = LocalContext.current
    val target by NavHandoff.handedOff.collectAsState()
    val guidance by InAppNav.guidance.collectAsState()
    val shown = HandedOffRoute.shown(target, guidance != null, System.currentTimeMillis())
    // What is drawn changes with the destination, not with every fix.
    val wanted = shown?.let { Triple(it.lat, it.lng, it.at) }
    val language = InAppNav.locale(context).language

    var marker by remember { mutableStateOf<Marker?>(null) }
    var drawn by remember { mutableStateOf<DirectionsRoute?>(null) }

    fun clear() {
        if (drawn != null) runCatching { navRoute?.removeRoute() }
        drawn = null
        marker?.let { m -> runCatching { map?.removeMarker(m) } }
        marker = null
    }

    LaunchedEffect(map, navRoute, wanted, language) {
        val line = navRoute ?: return@LaunchedEffect
        val mapRef = map ?: return@LaunchedEffect
        clear()
        val dest = shown ?: return@LaunchedEffect
        Log.i(HandedOffRoute.TAG, "handed off: \"${dest.name}\" pos=${dest.lat},${dest.lng} q=${dest.query}")
        val to = HandedOffRoute.resolve(dest) ?: run {
            Log.w(HandedOffRoute.TAG, "no position for \"${dest.name}\": nothing to draw")
            return@LaunchedEffect
        }
        Log.i(HandedOffRoute.TAG, "drawing to ${to.latitude()},${to.longitude()}")
        marker = runCatching {
            mapRef.addMarker(MarkerOptions().position(LatLng(to.latitude(), to.longitude())))
        }.getOrNull()
        var lastRequest = 0L
        var failures = 0
        var offSince = 0L
        var progress: RouteProgress? = null
        while (true) {
            val fix = LocationFeed.location.value
            val now = System.currentTimeMillis()
            if (fix != null) {
                val line0 = progress
                val at = line0?.locate(fix.latitude, fix.longitude)
                // Arrived: the trip is over and the line has nothing left to say.
                if (line0 != null && at != null &&
                    line0.lengthM - at.alongM < HandedOffRoute.ARRIVED_M &&
                    at.offM < HandedOffRoute.ARRIVED_M
                ) {
                    clear()
                    return@LaunchedEffect
                }
                val off = at != null && at.offM > HandedOffRoute.OFF_ROUTE_M
                if (off) { if (offSince == 0L) offSince = now } else offSince = 0L
                val stale = off && now - offSince > HandedOffRoute.OFF_ROUTE_MS
                if ((drawn == null || stale) && now - lastRequest > HandedOffRoute.gap(failures)) {
                    lastRequest = now
                    val route = withContext(Dispatchers.IO) {
                        runCatching {
                            valhallaRoute(
                                Point.fromLngLat(fix.longitude, fix.latitude),
                                to,
                                language
                            ).routes.firstOrNull()
                        }.getOrNull()
                    }
                    if (route == null) {
                        failures++
                        Log.w(HandedOffRoute.TAG, "no route from the router (failure ${failures})")
                    } else {
                        Log.i(HandedOffRoute.TAG, "line to \"${dest.name}\": ${route.distance.toInt()} m, ${route.duration.toInt()} s")
                        failures = 0
                        offSince = 0L
                        drawn = route
                        runCatching { line.addRoute(route) }
                        progress = route.geometry?.let { Polyline.decode(it, 6) }
                            ?.takeIf { it.size > 1 }
                            ?.let { RouteProgress(it) }
                    }
                }
            }
            delay(HandedOffRoute.TICK_MS)
        }
    }

    // The tile or the page going away takes its line and its marker with it.
    DisposableEffect(map, navRoute) { onDispose { clear() } }
}
