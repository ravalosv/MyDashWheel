// MINE: ver la ruta entera de un vistazo (#23). La cámara encuadra todos los
// puntos de la línea que el mapa dibuja ([RouteFeed]), sin inclinación ni giro;
// solo hay botón mientras esa línea exista.
package com.openauto.dash.mine

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

/** The box round a route: its south-west and north-east corners, in degrees. */
internal data class RouteBox(val south: Double, val west: Double, val north: Double, val east: Double)

internal object MapRouteOverview {
    /** Room round the route, in pixels, so its ends are not under the panel's edges. */
    private const val PADDING = 48

    /** How long the camera takes to the whole route: enough to follow it, short enough not to be in the way. */
    private const val FLAT_MS = 700

    /** How far off a landed camera may be and still count as framed: 200 m and four tenths of a zoom. */
    private const val SETTLED_M = 200.0
    private const val SETTLED_ZOOM = 0.4

    /** A camera that lands elsewhere is put back once more. */
    private const val RETRIES = 1

    /** The smallest box holding every point of [line]; null when there is nothing to frame. */
    internal fun bounds(line: RouteLine?): RouteBox? {
        val points = line?.points ?: return null
        val first = points.firstOrNull() ?: return null
        var south = first.first
        var north = first.first
        var west = first.second
        var east = first.second
        for ((lat, lon) in points) {
            if (lat < south) south = lat
            if (lat > north) north = lat
            if (lon < west) west = lon
            if (lon > east) east = lon
        }
        return RouteBox(south, west, north, east)
    }

    /**
     * Frames the whole of [line] on [map], flat and facing north. The camera
     * stops following the car first: still tracking, the component takes it
     * back to the car on the next fix —mid-animation— and the route never fills
     * the screen. Only after that does the «Centrar» button (#17) come up to
     * bring the camera back.
     */
    fun fit(map: MapLibreMap?, line: RouteLine?) {
        val box = bounds(line) ?: return
        if (map == null) return
        val corners = LatLngBounds.Builder()
            .include(LatLng(box.south, box.west))
            .include(LatLng(box.north, box.east))
            .build()
        MapRecentering.stopFollowing(map)
        // Whatever camera wasn't finished —the route's own arrival, the tracking transition that
        // Iniciar started, a driver's pan— would land on top of ours and undo it: the map zoomed out
        // but still on the car. Stopped, ours is the only one going (#23, second report).
        runCatching { map.cancelTransitions() }
        // The tilt and the bearing are asked for, not taken from the map: the two-argument call
        // computes the camera for the view it finds —tilted 45° and turned with the car— and the
        // result, applied flat, lands zoomed out but off-centre. That was the second report's
        // «two taps»: the first tap only flattened the map, the second one fitted it (#23, third
        // report).
        val fitted = runCatching {
            map.getCameraForLatLngBounds(corners, intArrayOf(PADDING, PADDING, PADDING, PADDING), 0.0, 0.0)
        }.getOrNull()
        if (fitted == null) {
            // Too early for a fitted camera (the view not laid out yet): the bounds update still moves.
            runCatching { map.animateCamera(CameraUpdateFactory.newLatLngBounds(corners, PADDING)) }
            return
        }
        val wanted = fitted.target ?: return
        // Flat, so the frame just computed for a flat view is exactly what is asked for.
        val flat = CameraPosition.Builder()
            .target(wanted)
            .zoom(fitted.zoom)
            .tilt(0.0)
            .bearing(0.0)
            .build()
        go(map, flat, wanted, RETRIES)
    }

    /** Moves the camera to [flat], and once the map rests checks it landed [wanted]; [tries] bounds the retries. */
    private fun go(map: MapLibreMap, flat: CameraPosition, wanted: LatLng, tries: Int) {
        runCatching { map.animateCamera(CameraUpdateFactory.newCameraPosition(flat), FLAT_MS) }
        if (tries <= 0) return
        val listener = object : MapLibreMap.OnCameraIdleListener {
            override fun onCameraIdle() {
                map.removeOnCameraIdleListener(this)
                val now = map.cameraPosition
                val at = now.target
                val lost = at == null || at.distanceTo(wanted) > SETTLED_M ||
                    kotlin.math.abs(now.zoom - flat.zoom) > SETTLED_ZOOM
                if (lost) go(map, flat, wanted, tries - 1)
            }
        }
        map.addOnCameraIdleListener(listener)
    }
}
